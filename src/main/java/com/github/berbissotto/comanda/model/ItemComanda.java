package com.github.berbissotto.comanda.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "item_comanda")
public class ItemComanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private Comanda comanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    // Nome e preço do momento da venda
    @Column(nullable = false)
    private String nomeProduto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
