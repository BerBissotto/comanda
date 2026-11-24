package com.github.berbissotto.comanda.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fonte de dados que "fixa" a empresa em cada conexão emprestada do pool único.
 *
 * <p>Toda conexão, ao ser entregue, recebe EXPLICITAMENTE o estado da requisição atual, sem
 * depender de como a conexão foi devolvida por quem a usou antes:
 * <ul>
 *   <li>com empresa: {@code SET ROLE empresa_N} + {@code search_path = empresa_N}. A partir daí o
 *       próprio PostgreSQL só deixa essa sessão tocar nas tabelas dessa empresa; sem permissão
 *       para outros schemas nem para public.usuario;</li>
 *   <li>sem empresa (central): {@code RESET ROLE} + {@code search_path = public}.</li>
 * </ul>
 * Se a preparação falhar, a conexão é descartada e o erro sobe: nunca se usa uma conexão em
 * estado incerto. Ao devolver ao pool, o estado também é limpo (segunda barreira).
 */
public class TenantDataSource extends DelegatingDataSource {

    static final String SQL_CENTRAL = "RESET ROLE; SET search_path TO public";

    public TenantDataSource(DataSource alvo) {
        super(alvo);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return preparar(super.getConnection());
    }

    @Override
    public Connection getConnection(String usuario, String senha) throws SQLException {
        return preparar(super.getConnection(usuario, senha));
    }

    static String sqlDaEmpresa(String schema) {
        if (!TenantContext.valido(schema)) {
            throw new IllegalArgumentException("Nome de empresa inválido");
        }
        return "SET ROLE \"" + schema + "\"; SET search_path TO \"" + schema + "\"";
    }

    private Connection preparar(Connection real) throws SQLException {
        String schema = TenantContext.atual();
        String sql = schema == null ? SQL_CENTRAL : sqlDaEmpresa(schema);
        try (Statement comando = real.createStatement()) {
            comando.execute(sql);
        } catch (SQLException | RuntimeException erro) {
            fecharSemFalhar(real);
            throw erro;
        }
        return (Connection) Proxy.newProxyInstance(
                TenantDataSource.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new LimpaAoFechar(real));
    }

    private static void fecharSemFalhar(Connection conexao) {
        try {
            conexao.close();
        } catch (SQLException ignorado) {
            // a causa original é a que importa
        }
    }

    /** Repassa tudo para a conexão real; no close(), volta ao estado central antes de devolver. */
    private static final class LimpaAoFechar implements InvocationHandler {

        private final Connection real;

        LimpaAoFechar(Connection real) {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method metodo, Object[] args) throws Throwable {
            String nome = metodo.getName();
            if (metodo.getParameterCount() == 0 && nome.equals("close")) {
                limpar();
                real.close();
                return null;
            }
            if (metodo.getParameterCount() == 0 && nome.equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            if (metodo.getParameterCount() == 1 && nome.equals("equals")) {
                return proxy == args[0];
            }
            try {
                return metodo.invoke(real, args);
            } catch (InvocationTargetException erro) {
                throw erro.getCause();
            }
        }

        private void limpar() {
            try {
                if (!real.isClosed()) {
                    try (Statement comando = real.createStatement()) {
                        comando.execute(SQL_CENTRAL);
                    }
                }
            } catch (SQLException ignorado) {
                // ex.: transação abortada. Sem problema: a próxima entrega da conexão reaplica o
                // estado completo antes de qualquer uso.
            }
        }
    }
}
