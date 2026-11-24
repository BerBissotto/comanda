package com.github.berbissotto.comanda.tenant;

import java.util.regex.Pattern;

/**
 * Empresa (schema) da requisição que está sendo atendida nesta thread.
 *
 * <p>Sem empresa definida = contexto CENTRAL: só enxerga o schema public (tabela usuario), usado
 * no login e na tela de Usuários. O valor só pode ser definido a partir do id do usuário
 * autenticado (veja {@link TenantFilter}); nunca a partir de parâmetro, cabeçalho ou formulário.
 *
 * <p>O nome do schema vai para comandos SQL entre aspas, por isso só é aceito o formato exato
 * {@code empresa_<número>}: qualquer outro valor é recusado aqui, antes de chegar ao banco.
 */
public final class TenantContext {

    private static final Pattern FORMATO_VALIDO = Pattern.compile("empresa_[1-9][0-9]{0,17}");
    private static final ThreadLocal<String> ATUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Nome do schema/papel da empresa de um usuário (ex.: id 7 -> empresa_7). */
    public static String schemaDe(Long idUsuario) {
        if (idUsuario == null || idUsuario <= 0) {
            throw new IllegalArgumentException("Id de usuário inválido para definir a empresa");
        }
        return "empresa_" + idUsuario;
    }

    public static boolean valido(String schema) {
        return schema != null && FORMATO_VALIDO.matcher(schema).matches();
    }

    public static void definir(String schema) {
        if (!valido(schema)) {
            throw new IllegalArgumentException("Nome de empresa inválido");
        }
        ATUAL.set(schema);
    }

    /** Schema da empresa atual, ou null no contexto central. */
    public static String atual() {
        return ATUAL.get();
    }

    public static void limpar() {
        ATUAL.remove();
    }
}
