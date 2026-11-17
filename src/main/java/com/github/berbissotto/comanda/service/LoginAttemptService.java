package com.github.berbissotto.comanda.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login erradas por e-mail (existente ou não, para não
 * revelar quais e-mails estão cadastrados). Depois de {@code max-attempts}
 * falhas seguidas o e-mail fica bloqueado por {@code lock-duration}.
 * O estado fica em memória: reiniciar a aplicação zera os contadores.
 */
@Service
public class LoginAttemptService {

    private static final int LIMITE_DE_REGISTROS = 10_000;

    private record Registro(int falhas, Instant bloqueadoAte, Instant ultimaFalha) {
    }

    private final ConcurrentHashMap<String, Registro> registros = new ConcurrentHashMap<>();
    private final int maxTentativas;
    private final Duration duracaoBloqueio;
    private final Clock relogio;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxTentativas,
            @Value("${app.security.login.lock-duration:15m}") Duration duracaoBloqueio) {
        this(maxTentativas, duracaoBloqueio, Clock.systemUTC());
    }

    LoginAttemptService(int maxTentativas, Duration duracaoBloqueio, Clock relogio) {
        this.maxTentativas = Math.max(1, maxTentativas);
        this.duracaoBloqueio = duracaoBloqueio;
        this.relogio = relogio;
    }

    public void registrarFalha(String email) {
        String chave = chave(email);
        Instant agora = relogio.instant();
        limparExpirados(agora);
        registros.compute(chave, (k, atual) -> {
            // falhas antigas (fora da janela de bloqueio) não contam
            int falhas = (atual == null || atual.ultimaFalha().plus(duracaoBloqueio).isBefore(agora))
                    ? 1 : atual.falhas() + 1;
            Instant ate = falhas >= maxTentativas ? agora.plus(duracaoBloqueio) : null;
            return new Registro(falhas, ate, agora);
        });
    }

    public void registrarSucesso(String email) {
        registros.remove(chave(email));
    }

    public boolean estaBloqueado(String email) {
        Registro registro = registros.get(chave(email));
        return registro != null
                && registro.bloqueadoAte() != null
                && registro.bloqueadoAte().isAfter(relogio.instant());
    }

    private void limparExpirados(Instant agora) {
        if (registros.size() < LIMITE_DE_REGISTROS) {
            return;
        }
        registros.entrySet().removeIf(e -> e.getValue().ultimaFalha().plus(duracaoBloqueio).isBefore(agora));
    }

    private String chave(String email) {
        String limpo = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return limpo.length() > 254 ? limpo.substring(0, 254) : limpo;
    }
}
