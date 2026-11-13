package com.github.berbissotto.comanda.controller;

import com.github.berbissotto.comanda.dto.ProdutoForm;
import com.github.berbissotto.comanda.dto.ProdutoResponse;
import com.github.berbissotto.comanda.model.Produto;
import com.github.berbissotto.comanda.repository.ProdutoRepository;
import com.github.berbissotto.comanda.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    @PostMapping("/novo")
    public String salvarProduto(@ModelAttribute("produto") ProdutoForm form, BindingResult result,
                                @RequestParam(value = "imagens", required = false)List<MultipartFile> arquivos,
                                Model model) {
        if (result.hasErrors()) {
            return "produtos/form";
        }

        try {
            Produto produto = new Produto();
            return "redirect:/produtos";
        } catch (IOException | IllegalArgumentException exception) {
            model.addAttribute("erro", exception.getMessage());
            return "produtos/form";
        }
    }
}
