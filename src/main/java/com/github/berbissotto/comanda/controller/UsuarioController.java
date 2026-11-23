package com.github.berbissotto.comanda.controller;

import com.github.berbissotto.comanda.dto.UsuarioForm;
import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.service.UsuarioLogado;
import com.github.berbissotto.comanda.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cadastro de usuários de acesso ao sistema. Fluxo: lista -> cadastro (novo ou edição) -> lista.
 * Só o perfil ADMIN chega aqui (SecurityConfig) e cada ação confere de novo no banco.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Authentication autenticacao, Model model) {
        administradorAtual(autenticacao);
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuario/lista";
    }

    @GetMapping("/form")
    public String novo(Authentication autenticacao, Model model) {
        administradorAtual(autenticacao);
        return formulario(model, new UsuarioForm(), false);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Authentication autenticacao, Model model) {
        administradorAtual(autenticacao);
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return formulario(model, UsuarioForm.from(usuario), true);
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("usuario") UsuarioForm form,
                         BindingResult result,
                         Authentication autenticacao,
                         Model model,
                         RedirectAttributes redirect) {
        Long idLogado = administradorAtual(autenticacao);
        boolean edicao = form.getId() != null;

        if (result.hasErrors()) {
            // erro de conversão (ex.: situação adulterada) vem com mensagem técnica
            String mensagem = result.hasFieldErrors("ativo")
                    ? "Informe a situação do usuário"
                    : result.getAllErrors().get(0).getDefaultMessage();
            return formularioComErro(model, edicao, mensagem);
        }

        try {
            usuarioService.salvar(form, idLogado);
        } catch (IllegalArgumentException exception) {
            return formularioComErro(model, edicao, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            // dois cadastros com o mesmo e-mail ao mesmo tempo
            return formularioComErro(model, edicao, "Já existe um usuário com este e-mail");
        }

        redirect.addFlashAttribute("sucesso", edicao ? "Alterações salvas." : "Usuário cadastrado.");
        return "redirect:/usuarios";
    }

    private String formulario(Model model, UsuarioForm form, boolean edicao) {
        model.addAttribute("usuario", form);
        return paginaDoFormulario(model, edicao);
    }

    private String formularioComErro(Model model, boolean edicao, String mensagem) {
        model.addAttribute("erro", mensagem);
        return paginaDoFormulario(model, edicao);
    }

    private String paginaDoFormulario(Model model, boolean edicao) {
        model.addAttribute("edicao", edicao);
        model.addAttribute("titulo", edicao ? "Editar usuário" : "Novo usuário");
        return "usuario/form";
    }

    // Quem está logado, conferido no banco: precisa continuar sendo ADMIN e ativo
    private Long administradorAtual(Authentication autenticacao) {
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioLogado logado)) {
            throw new AccessDeniedException("Acesso restrito a administradores");
        }
        usuarioService.exigirAdministradorAtivo(logado.getId());
        return logado.getId();
    }
}
