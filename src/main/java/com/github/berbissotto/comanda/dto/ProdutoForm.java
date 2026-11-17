package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Produto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProdutoForm {

    private Long id;
    @NotBlank(message = "Informe o nome do produto")
    @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres")
    private String nome;

    @NotNull(message = "Informe uma quantidade válida")
    @Min(value = 0, message = "Informe uma quantidade válida")
    @Max(value = 1_000_000, message = "Quantidade acima do permitido")
    private Integer quantidade;

    @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres")
    private String descricao;

    @Size(max = 60, message = "A categoria pode ter no máximo 60 caracteres")
    private String categoria;

    @NotNull(message = "Informe um preço válido")
    @DecimalMin(value = "0", message = "Informe um preço válido")
    @DecimalMax(value = "1000000", message = "Preço acima do permitido")
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
