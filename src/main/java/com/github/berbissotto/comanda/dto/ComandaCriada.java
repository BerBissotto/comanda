package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Comanda;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record ComandaCriada(Long id, int numero, String anoMes, String dataHora, BigDecimal total) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static ComandaCriada from(Comanda comanda) {
        return new ComandaCriada(comanda.getId(), comanda.getNumeroMes(), comanda.getAnoMes(),
                comanda.getDataHora().format(FORMATO), comanda.getTotal());
    }
}
