package com.github.berbissotto.comanda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corpo do POST /comandas. Os preços NÃO vêm do navegador: o servidor
 * busca cada produto pelo id e calcula o total.
 */
public record ComandaRequest(
        @Size(max = 20, message = "A mesa pode ter no máximo 20 caracteres") String mesa,
        @Size(max = 120, message = "O nome do cliente pode ter no máximo 120 caracteres") String cliente,
        @Size(max = 20, message = "O telefone pode ter no máximo 20 caracteres")
        @Pattern(regexp = "[0-9()+\\-\\s]*", message = "Telefone inválido") String telefone,
        @Size(max = 255, message = "O endereço pode ter no máximo 255 caracteres") String endereco,
        @Size(max = 500, message = "As observações podem ter no máximo 500 caracteres") String observacoes,
        @NotEmpty(message = "A comanda não tem itens")
        @Size(max = 200, message = "Itens demais na comanda") List<@Valid Item> itens
) {
    public record Item(Long produtoId, Integer quantidade) {
    }
}
