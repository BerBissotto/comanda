package com.github.berbissotto.comanda.controller;

import com.github.berbissotto.comanda.dto.ClienteResponse;
import com.github.berbissotto.comanda.dto.ComandaCriada;
import com.github.berbissotto.comanda.dto.ComandaRequest;
import com.github.berbissotto.comanda.service.ComandaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class ComandaController {

    @Autowired
    private ComandaService comandaService;

    // Tela final: o carrinho vive no navegador e a página monta a lista a partir dele
    @GetMapping("/comanda")
    public String finalizar() {
        return "comanda/finalizar";
    }

    @PostMapping("/comandas")
    @ResponseBody
    public ResponseEntity<ComandaCriada> criar(@RequestBody ComandaRequest request) {
        return ResponseEntity.ok(comandaService.finalizar(request));
    }

    // Preenchimento automático do cliente pelo nome
    @GetMapping("/clientes/busca")
    @ResponseBody
    public List<ClienteResponse> buscarClientes(@RequestParam(name = "q", defaultValue = "") String q) {
        return comandaService.buscarClientes(q).stream().map(ClienteResponse::from).toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> dadosInvalidos(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("erro", exception.getMessage()));
    }
}
