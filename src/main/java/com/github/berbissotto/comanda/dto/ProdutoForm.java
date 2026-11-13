package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Produto;

public class ProdutoForm {

    private Long id;
    private String nome;
    private Integer quantidade;
    private String descricao;
    private String categoria;
    private Double preco;

    public static ProdutoForm from(Produto produto) {
        ProdutoForm form = new ProdutoForm();
        form.setId(produto.getId());
        form.setNome(produto.getNome());
        form.setQuantidade(produto.getQuantidade());
        form.setDescricao(produto.getDescricao());
        form.setCategoria(produto.getCategoria());
        form.setPreco(produto.getPreco());
        return form;
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

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }
}
