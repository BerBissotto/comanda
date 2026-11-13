package com.github.berbissotto.comanda.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;
    private String categoria;
    private int quantidade;
    private double preco;

    // Uma única foto por produto, guardada direto na tabela
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "foto", columnDefinition = "bytea")
    private byte[] foto;

    @Column(name = "foto_tipo")
    private String fotoTipo; // ex.: image/png

    @Override
    public String toString() {
        return "Produto{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", categoria='" + categoria + '\'' +
                ", quantidade=" + quantidade +
                ", descricao='" + descricao + '\'' +
                ", preco=" + preco +
                ", temFoto=" + (foto != null) +
                '}';
    }
}
