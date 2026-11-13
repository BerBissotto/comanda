package com.github.berbissotto.comanda.dto;

import java.util.List;

/**
 * Corpo do POST /comandas. Os preços NÃO vêm do navegador: o servidor
 * busca cada produto pelo id e calcula o total.
 */
public record ComandaRequest(
        String mesa,
        String cliente,
        String telefone,
        String endereco,
        String observacoes,
        List<Item> itens
) {
    public record Item(Long produtoId, Integer quantidade) {
    }
}
