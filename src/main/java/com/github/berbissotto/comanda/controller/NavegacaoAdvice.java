package com.github.berbissotto.comanda.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Disponibiliza "ehAdmin" para todas as telas: o menu só mostra a opção Usuários para o perfil
 * ADMIN. É só apresentação; quem barra o acesso de verdade é o SecurityConfig e o controller.
 */
@ControllerAdvice
public class NavegacaoAdvice {

    @ModelAttribute("ehAdmin")
    public boolean ehAdmin() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null
                && autenticacao.isAuthenticated()
                && autenticacao.getAuthorities().stream()
                        .anyMatch(autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()));
    }
}
