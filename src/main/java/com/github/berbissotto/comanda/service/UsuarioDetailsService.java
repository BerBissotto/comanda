package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenha())
                .roles("USER")
                .build();
    }
}
