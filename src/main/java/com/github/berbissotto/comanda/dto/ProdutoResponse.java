package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Produto;

public record ProdutoResponse(
        Long id,
        String nome,
        int quantidade,
        String descricao,
        String categoria,
        double preco,
        String fotoUrl
) {
    public static ProdutoResponse from(Produto produto) {
        String fotoUrl = produto.getFoto() == null
                ? null
                : "/produtos/" + produto.getId() + "/foto";
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getQuantidade(),
                produto.getDescricao(), produto.getCategoria(), produto.getPreco(), fotoUrl);
    }
}
