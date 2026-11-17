package com.github.berbissotto.comanda.config;

import com.github.berbissotto.comanda.service.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Alimenta o contador de tentativas com os eventos do Spring Security. */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService tentativas;

    public LoginAttemptListener(LoginAttemptService tentativas) {
        this.tentativas = tentativas;
    }

    @EventListener
    public void aoFalhar(AuthenticationFailureBadCredentialsEvent evento) {
        tentativas.registrarFalha(evento.getAuthentication().getName());
    }

    @EventListener
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        tentativas.registrarSucesso(evento.getAuthentication().getName());
    }
}
