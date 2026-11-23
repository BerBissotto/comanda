package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.model.Role;
import com.github.berbissotto.comanda.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * Usuário autenticado. Além do e-mail, guarda o id (para reconhecer "o próprio usuário" mesmo que
 * o e-mail mude) e carrega o perfil como autoridade ROLE_ADMIN / ROLE_USER.
 * Usuário inativo vira "desabilitado": o Spring Security recusa o login dele.
 */
public class UsuarioLogado extends User {

    private static final long serialVersionUID = 1L;

    private final Long id;

    public UsuarioLogado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenha(), usuario.isAtivo(), true, true, true,
                autoridades(usuario.getRole()));
        this.id = usuario.getId();
    }

    public Long getId() {
        return id;
    }

    private static List<GrantedAuthority> autoridades(Role role) {
        Role efetivo = role == null ? Role.USER : role;
        return List.of(new SimpleGrantedAuthority("ROLE_" + efetivo.name()));
    }
}
