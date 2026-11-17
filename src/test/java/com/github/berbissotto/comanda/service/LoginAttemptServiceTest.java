package com.github.berbissotto.comanda.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private static class RelogioFalso extends Clock {
        Instant agora = Instant.parse("2026-11-16T12:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    void bloqueiaDepoisDoLimiteEDesbloqueiaAposOTempo() {
        RelogioFalso relogio = new RelogioFalso();
        LoginAttemptService s = new LoginAttemptService(3, Duration.ofMinutes(15), relogio);

        s.registrarFalha("A@x.com");
        s.registrarFalha("a@x.com");
        assertFalse(s.estaBloqueado("a@x.com"));
        s.registrarFalha(" a@x.com ");
        assertTrue(s.estaBloqueado("A@X.COM"));
        assertFalse(s.estaBloqueado("outro@x.com"));

        relogio.agora = relogio.agora.plus(Duration.ofMinutes(16));
        assertFalse(s.estaBloqueado("a@x.com"));
    }

    @Test
    void sucessoZeraOContador() {
        LoginAttemptService s = new LoginAttemptService(2, Duration.ofMinutes(15), new RelogioFalso());
        s.registrarFalha("a@x.com");
        s.registrarSucesso("a@x.com");
        s.registrarFalha("a@x.com");
        assertFalse(s.estaBloqueado("a@x.com"));
    }

    @Test
    void emailInexistenteTambemBloqueia() {
        LoginAttemptService s = new LoginAttemptService(1, Duration.ofMinutes(15), new RelogioFalso());
        s.registrarFalha("naoexiste@x.com");
        assertTrue(s.estaBloqueado("naoexiste@x.com"));
    }
}
