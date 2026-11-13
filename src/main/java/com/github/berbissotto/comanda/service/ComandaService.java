package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.dto.ComandaCriada;
import com.github.berbissotto.comanda.dto.ComandaRequest;
import com.github.berbissotto.comanda.model.Cliente;
import com.github.berbissotto.comanda.model.Comanda;
import com.github.berbissotto.comanda.model.ItemComanda;
import com.github.berbissotto.comanda.model.Produto;
import com.github.berbissotto.comanda.repository.ClienteRepository;
import com.github.berbissotto.comanda.repository.ComandaRepository;
import com.github.berbissotto.comanda.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComandaService {

    private static final int QUANTIDADE_MAXIMA = 999;

    @Autowired
    private ComandaRepository comandaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Transactional
    public ComandaCriada finalizar(ComandaRequest request) {
        String nomeCliente = limpar(request.cliente());
        String telefone = limpar(request.telefone());
        String endereco = limpar(request.endereco());

        if (nomeCliente.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome do cliente");
        }
        if (telefone.isEmpty()) {
            throw new IllegalArgumentException("Informe o telefone do cliente");
        }
        if (endereco.isEmpty()) {
            throw new IllegalArgumentException("Informe o endereço do cliente");
        }
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("A comanda não tem itens");
        }

        // Soma quantidades de um mesmo produto enviado mais de uma vez
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ComandaRequest.Item item : request.itens()) {
            if (item == null || item.produtoId() == null || item.quantidade() == null
                    || item.quantidade() < 1 || item.quantidade() > QUANTIDADE_MAXIMA) {
                throw new IllegalArgumentException("Item inválido na comanda");
            }
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        Map<Long, Produto> produtos = produtoRepository.findAllById(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Produto::getId, Function.identity()));
        if (produtos.size() != quantidades.size()) {
            throw new IllegalArgumentException("Um dos produtos da comanda não existe mais");
        }

        YearMonth agora = YearMonth.now();
        String anoMes = agora.toString(); // ex.: 2026-11

        Comanda comanda = new Comanda();
        comanda.setAnoMes(anoMes);
        comanda.setNumeroMes(comandaRepository.maiorNumeroDoMes(anoMes) + 1);
        comanda.setDataHora(LocalDateTime.now());
        comanda.setMesa(limpar(request.mesa()).isEmpty() ? null : limpar(request.mesa()));
        comanda.setClienteNome(nomeCliente);
        comanda.setTelefone(telefone);
        comanda.setEndereco(endereco);
        comanda.setObservacoes(limpar(request.observacoes()).isEmpty() ? null : limpar(request.observacoes()));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entrada : quantidades.entrySet()) {
            Produto produto = produtos.get(entrada.getKey());
            ItemComanda item = new ItemComanda();
            item.setProduto(produto);
            item.setNomeProduto(produto.getNome());
            item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()).setScale(2, java.math.RoundingMode.HALF_UP));
            item.setQuantidade(entrada.getValue());
            comanda.adicionarItem(item);
            total = total.add(item.getSubtotal());
        }
        comanda.setTotal(total);

        // Guarda/atualiza o cliente para o preenchimento automático nas próximas comandas
        Cliente cliente = clienteRepository.findFirstByNomeIgnoreCase(nomeCliente).orElseGet(Cliente::new);
        cliente.setNome(nomeCliente);
        cliente.setTelefone(telefone);
        cliente.setEndereco(endereco);
        comanda.setCliente(clienteRepository.save(cliente));

        return ComandaCriada.from(comandaRepository.save(comanda));
    }

    public List<Cliente> buscarClientes(String trecho) {
        String busca = limpar(trecho);
        if (busca.isEmpty()) {
            return List.of();
        }
        return clienteRepository.findTop8ByNomeContainingIgnoreCaseOrderByNomeAsc(busca);
    }

    private String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
