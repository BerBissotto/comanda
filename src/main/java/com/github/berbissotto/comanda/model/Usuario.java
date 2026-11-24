package com.github.berbissotto.comanda.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    // hash BCrypt, nunca a senha em texto
    @Column(nullable = false)
    private String senha;

    // Os valores padrão no banco (db/central/V1__usuario.sql) permitem adicionar as colunas numa
    // tabela que já tem usuários: quem já existia passa a ser USER e ativo (o ADMIN é definido por SQL).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private Role role = Role.USER;

    // Usuário inativo não consegue entrar no sistema
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativo = true;
}
