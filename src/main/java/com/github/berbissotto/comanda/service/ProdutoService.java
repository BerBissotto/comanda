package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.model.Produto;
import com.github.berbissotto.comanda.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id);
    }

    public List<Produto> buscarPorIds(List<Long> ids) {
        return produtoRepository.findAllById(ids);
    }

    public Produto salvar(Produto produto) {
        return produtoRepository.save(produto);
    }

    public List<Produto> buscarComFiltros(String nome, Double valorMin, Double valorMax, String categoria) {
        return produtoRepository.buscarComFiltros(nome, valorMin, valorMax, categoria);
    }

    public Page<Produto> buscarComFiltros(String nome, Double valorMin, Double valorMax, String categoria, Pageable pageable) {
        return produtoRepository.buscarComFiltros(nome, valorMin, valorMax, categoria, pageable);
    }

    public Page<Produto> listarProdutos(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }

    public void deletar(Long id){
        produtoRepository.deleteById(id);
    }

    public Produto atualizar(Long id, Produto novoProduto){
        return produtoRepository.findById(id).map(produto -> {
            produto.setNome(novoProduto.getNome());
            produto.setPreco(novoProduto.getPreco());
            produto.setQuantidade(novoProduto.getQuantidade());
            produto.setDescricao(novoProduto.getDescricao());
            produto.setCategoria(novoProduto.getCategoria());
            // só troca a foto se uma nova foi enviada
            if (novoProduto.getFoto() != null) {
                produto.setFoto(novoProduto.getFoto());
                produto.setFotoTipo(novoProduto.getFotoTipo());
            }
            return produtoRepository.save(produto);
        }).orElseGet(()->{
            novoProduto.setId(id);
            return produtoRepository.save(novoProduto);
        });
    }
}
