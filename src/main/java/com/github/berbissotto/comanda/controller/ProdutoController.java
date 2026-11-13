package com.github.berbissotto.comanda.controller;

import com.github.berbissotto.comanda.dto.ProdutoForm;
import com.github.berbissotto.comanda.dto.ProdutoResponse;
import com.github.berbissotto.comanda.model.Produto;
import com.github.berbissotto.comanda.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoService produtoService;

    @GetMapping("/form")
    public String novoProduto(Model model) {
        model.addAttribute("produto", new ProdutoForm());
        return "produto/form";
    }

    @GetMapping
    public String listaProdutos(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());
        return "produto/lista";
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscaPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id)
                .map(ProdutoResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Devolve a foto do produto (usada em <img th:src="@{/produtos/{id}/foto(id=${p.id})}">)
    @GetMapping("/{id}/foto")
    @ResponseBody
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        return produtoService.buscarPorId(id)
                .filter(produto -> produto.getFoto() != null)
                .map(produto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(produto.getFotoTipo()))
                        .body(produto.getFoto()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/novo")
    public String salvarProduto(@ModelAttribute("produto") ProdutoForm form,
                                BindingResult result,
                                @RequestParam(value = "foto", required = false) MultipartFile arquivo,
                                Model model) {
        if (result.hasErrors()) {
            return "produto/form";
        }

        try {
            if (form.getNome() == null || form.getNome().isBlank()) {
                throw new IllegalArgumentException("Informe o nome do produto");
            }
            if (form.getPreco() == null || form.getPreco() < 0) {
                throw new IllegalArgumentException("Informe um preço válido");
            }
            if (form.getQuantidade() == null || form.getQuantidade() < 0) {
                throw new IllegalArgumentException("Informe uma quantidade válida");
            }

            Produto produto = new Produto();
            produto.setNome(form.getNome());
            produto.setQuantidade(form.getQuantidade());
            produto.setDescricao(form.getDescricao());
            produto.setCategoria(form.getCategoria());
            produto.setPreco(form.getPreco());

            if (arquivo != null && !arquivo.isEmpty()) {
                String tipo = arquivo.getContentType();
                if (tipo == null || !tipo.startsWith("image/")) {
                    throw new IllegalArgumentException("O arquivo enviado precisa ser uma imagem");
                }
                produto.setFoto(arquivo.getBytes());
                produto.setFotoTipo(tipo);
            }

            produtoService.salvar(produto);
            return "redirect:/produtos";
        } catch (IOException | IllegalArgumentException exception) {
            model.addAttribute("erro", exception.getMessage());
            return "produto/form";
        }
    }
}
