package com.github.berbissotto.comanda.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Arrays;

@Entity
@Data
@NoArgsConstructor
public class Imagem {

    @Id
    @GeneratedValue
    private Long id;

    private String nome;
    private String tipo;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "dados", columnDefinition = "bytea", nullable = false)
    private byte[] dados;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Override
    public String toString() {
        return "Imagem{id=" + ", nome=" + nome + ", tipo=" + tipo + "'}";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public byte[] getDados() {
        return dados;
    }

    public void setDados(byte[] dados) {
        this.dados = dados;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }
}
