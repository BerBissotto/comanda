package com.github.berbissotto.comanda.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "comanda",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ano_mes", "numero_mes"}))
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número sequencial dentro do mês (ex.: 7 em "2026-11")
    @Column(name = "ano_mes", nullable = false, length = 7)
    private String anoMes;

    @Column(name = "numero_mes", nullable = false)
    private int numeroMes;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 20)
    private String mesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Cópia dos dados no momento da venda: mudar o cadastro do cliente não altera comandas antigas
    @Column(nullable = false, length = 120)
    private String clienteNome;

    @Column(length = 20)
    private String telefone;

    @Column(length = 255)
    private String endereco;

    @Column(length = 500)
    private String observacoes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemComanda> itens = new ArrayList<>();

    public void adicionarItem(ItemComanda item) {
        item.setComanda(this);
        itens.add(item);
    }
}
