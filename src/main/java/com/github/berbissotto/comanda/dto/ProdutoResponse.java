package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Imagem;
import com.github.berbissotto.comanda.model.Produto;

import java.util.ArrayList;
import java.util.List;

public record ProdutoResponse (
        Long id,
        String nome,
        int quantidade,
        String descricao,
        String categoria,
        double preco,
        List<Long> imagemId
)

{public static ProdutoResponse from(Produto produto){
    List<Long> imagem = produto.getImagens() == null
        ? List.of()
            :  produto.getImagens().stream().map(Imagem::getId).toList();
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getQuantidade(), produto.getDescricao(), produto.getCategoria(), produto.getPreco(), imagem);
}}
