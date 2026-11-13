package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Cliente;

public record ClienteResponse(Long id, String nome, String telefone, String endereco) {

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(), cliente.getEndereco());
    }
}
