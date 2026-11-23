package com.github.berbissotto.comanda.dto;

import com.github.berbissotto.comanda.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Dados do cadastro de usuário. Não tem o perfil (ROLE) de propósito: quem cadastra pela tela
 * nunca consegue criar nem promover um ADMIN. A senha só é obrigatória no cadastro novo; na
 * edição, em branco significa "manter a senha atual" (a regra fica em UsuarioService).
 */
@Getter
@Setter
public class UsuarioForm {

    private Long id;

    @NotBlank(message = "Informe o nome da empresa")
    @Size(max = 120, message = "O nome da empresa pode ter no máximo 120 caracteres")
    private String nome;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 160, message = "O e-mail pode ter no máximo 160 caracteres")
    private String email;

    private String senha;

    @NotNull(message = "Informe a situação do usuário")
    private Boolean ativo = Boolean.TRUE;

    public static UsuarioForm from(Usuario usuario) {
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.getId());
        form.setNome(usuario.getNome());
        form.setEmail(usuario.getEmail());
        form.setAtivo(usuario.isAtivo());
        // a senha (hash) nunca volta para a tela
        return form;
    }
}
