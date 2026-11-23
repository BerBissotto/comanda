package com.github.berbissotto.comanda.model;

/**
 * Perfil de acesso do usuário. ADMIN vê e usa a tela de Usuários; USER usa o restante do sistema.
 * O perfil ADMIN só é concedido direto no banco (veja sql/admin-usuario.sql): a tela de
 * cadastro cria sempre usuários USER.
 */
public enum Role {
    ADMIN,
    USER
}
