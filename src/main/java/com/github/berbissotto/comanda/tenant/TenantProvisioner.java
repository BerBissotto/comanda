package com.github.berbissotto.comanda.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cria e mantém o espaço de dados de cada empresa: um papel (role) e um schema próprios,
 * {@code empresa_<id>}, com as tabelas definidas em {@code db/tenant/V*.sql}.
 *
 * <ul>
 *   <li>Criar a empresa de um usuário novo acontece na MESMA transação que grava o usuário: se
 *       qualquer passo falhar, nem o usuário nem a empresa existem (o PostgreSQL desfaz tudo,
 *       inclusive CREATE ROLE e CREATE SCHEMA).</li>
 *   <li>Mudanças de estrutura: crie um arquivo novo {@code V2__descricao.sql}, {@code V3__...}
 *       em db/tenant. Ao subir, a aplicação aplica em todas as empresas o que ainda não foi
 *       aplicado (controle na tabela schema_versao de cada empresa). Nunca edite um arquivo já
 *       publicado.</li>
 * </ul>
 * Tudo aqui roda no contexto central e como o papel de cada empresa para criar as tabelas, de
 * modo que as tabelas pertencem à empresa e a nenhuma outra.
 */
@Component
public class TenantProvisioner {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioner.class);
    private static final Pattern NOME_MIGRACAO = Pattern.compile("V(\\d+)__.+\\.sql");
    // Serializa migrações entre instâncias da aplicação subindo ao mesmo tempo
    private static final long CHAVE_TRAVA = 7_312_001L;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final List<Migracao> migracoesDaEmpresa;
    private final List<Resource> scriptsCentrais;

    public TenantProvisioner(DataSource dataSource, PlatformTransactionManager gerenteDeTransacao) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.transacao = new TransactionTemplate(gerenteDeTransacao);
        this.migracoesDaEmpresa = carregarMigracoes();
        this.scriptsCentrais = carregarCentrais();
    }

    /** Garante a tabela usuario no schema public (idempotente; instalação nova ou antiga). */
    public void migrarCentral() {
        exigirContextoCentral();
        transacao.executeWithoutResult(status -> {
            jdbc.execute("SELECT pg_advisory_xact_lock(" + CHAVE_TRAVA + ")");
            for (Resource script : scriptsCentrais) {
                executarScript(script);
            }
        });
    }

    /**
     * Cria (se não existir) e atualiza a empresa do usuário. Se já houver uma transação em
     * andamento, participa dela. Lança IllegalStateException com mensagem para o administrador.
     */
    public void provisionar(Long idUsuario) {
        exigirContextoCentral();
        String schema = TenantContext.schemaDe(idUsuario);
        try {
            transacao.executeWithoutResult(status -> {
                jdbc.execute("SELECT pg_advisory_xact_lock(" + CHAVE_TRAVA + ")");
                criarPapelESchema(schema);
                aplicarMigracoes(schema);
            });
        } catch (RuntimeException erro) {
            log.error("Falha ao preparar a empresa {}", schema, erro);
            throw new IllegalStateException("Não foi possível criar o espaço de dados da empresa. "
                    + "Confira se o script sql/01-preparar-banco.sql foi executado no banco "
                    + "e veja o log do servidor.", erro);
        }
    }

    /**
     * Para usuários criados fora da tela (ex.: o INSERT do administrador por SQL): cria a empresa
     * no primeiro login, se ainda não existir. Uma consulta barata quando já existe.
     */
    public void garantir(Long idUsuario) {
        exigirContextoCentral();
        String schema = TenantContext.schemaDe(idUsuario);
        if (!existe("SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)", schema)) {
            provisionar(idUsuario);
        }
    }

    /** No início da aplicação: leva todas as empresas à estrutura mais recente. */
    public void provisionarTodas() {
        exigirContextoCentral();
        List<Long> ids = jdbc.queryForList("SELECT id FROM usuario ORDER BY id", Long.class);
        int feitas = 0;
        for (Long id : ids) {
            provisionar(id);
            feitas++;
            if (feitas % 100 == 0) {
                log.info("Empresas verificadas: {}/{}", feitas, ids.size());
            }
        }
        log.info("Empresas prontas: {}", ids.size());
    }

    private void criarPapelESchema(String schema) {
        String nome = aspas(schema);
        if (!existe("SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = ?)", schema)) {
            jdbc.execute("CREATE ROLE " + nome + " NOLOGIN");
        }
        // A aplicação precisa poder executar SET ROLE para esta empresa (sem herdar direitos dela)
        if (!existe("SELECT pg_has_role(CURRENT_USER, ?, 'MEMBER')", schema)) {
            jdbc.execute("GRANT " + nome + " TO CURRENT_USER");
        }
        if (!existe("SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)", schema)) {
            jdbc.execute("CREATE SCHEMA " + nome + " AUTHORIZATION " + nome);
        }
    }

    private void aplicarMigracoes(String schema) {
        // Daqui até o RESET ROLE tudo roda como a própria empresa, dentro do schema dela
        jdbc.execute("SET LOCAL ROLE " + aspas(schema));
        jdbc.execute("SET LOCAL search_path TO " + aspas(schema));
        jdbc.execute("CREATE TABLE IF NOT EXISTS schema_versao ("
                + "versao integer PRIMARY KEY, arquivo varchar(200) NOT NULL, "
                + "aplicada_em timestamptz NOT NULL DEFAULT now())");
        Set<Integer> aplicadas = new HashSet<>(
                jdbc.queryForList("SELECT versao FROM schema_versao", Integer.class));
        for (Migracao migracao : migracoesDaEmpresa) {
            if (!aplicadas.contains(migracao.versao())) {
                executarScript(migracao.arquivo());
                jdbc.update("INSERT INTO schema_versao (versao, arquivo) VALUES (?, ?)",
                        migracao.versao(), migracao.arquivo().getFilename());
                log.info("Empresa {}: aplicada {}", schema, migracao.arquivo().getFilename());
            }
        }
        jdbc.execute("RESET ROLE");
        jdbc.execute("SET LOCAL search_path TO public");
    }

    private boolean existe(String sql, String schema) {
        return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, schema));
    }

    private void executarScript(Resource script) {
        jdbc.execute((ConnectionCallback<Void>) conexao -> {
            ScriptUtils.executeSqlScript(conexao, new EncodedResource(script, StandardCharsets.UTF_8));
            return null;
        });
    }

    private static String aspas(String schema) {
        if (!TenantContext.valido(schema)) {
            throw new IllegalArgumentException("Nome de empresa inválido");
        }
        return "\"" + schema + "\"";
    }

    private static void exigirContextoCentral() {
        if (TenantContext.atual() != null) {
            throw new IllegalStateException("A criação de empresas só pode rodar no contexto central");
        }
    }

    private static List<Migracao> carregarMigracoes() {
        try {
            Resource[] arquivos = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:db/tenant/V*__*.sql");
            List<Migracao> migracoes = new java.util.ArrayList<>();
            Set<Integer> versoes = new HashSet<>();
            for (Resource arquivo : arquivos) {
                Matcher casamento = NOME_MIGRACAO.matcher(String.valueOf(arquivo.getFilename()));
                if (!casamento.matches()) {
                    throw new IllegalStateException("Nome de migração inválido: " + arquivo.getFilename());
                }
                int versao = Integer.parseInt(casamento.group(1));
                if (!versoes.add(versao)) {
                    throw new IllegalStateException("Versão de migração repetida: V" + versao);
                }
                migracoes.add(new Migracao(versao, arquivo));
            }
            migracoes.sort(Comparator.comparingInt(Migracao::versao));
            return List.copyOf(migracoes);
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível ler db/tenant", erro);
        }
    }

    private static List<Resource> carregarCentrais() {
        try {
            Resource[] arquivos = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:db/central/V*__*.sql");
            return java.util.Arrays.stream(arquivos)
                    .sorted(Comparator.comparing(arquivo -> String.valueOf(arquivo.getFilename())))
                    .toList();
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível ler db/central", erro);
        }
    }

    private record Migracao(int versao, Resource arquivo) {
    }
}
