package com.github.berbissotto.comanda.config;

import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cria o primeiro usuário quando a tabela está vazia.
 * Defina APP_ADMIN_EMAIL e APP_ADMIN_SENHA para escolher as credenciais;
 * sem senha definida, uma senha aleatória é gerada e mostrada UMA vez no log.
 */
@Component
public class AdminInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@comanda.local}")
    private String email;

    @Value("${app.admin.senha:}")
    private String senha;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        boolean gerada = senha == null || senha.isBlank();
        String senhaFinal = gerada ? gerarSenha() : senha;

        Usuario usuario = new Usuario();
        usuario.setNome("Administrador");
        usuario.setEmail(email.trim());
        usuario.setSenha(passwordEncoder.encode(senhaFinal));
        usuarioRepository.save(usuario);

        if (gerada) {
            log.warn("Usuário inicial criado: {} / senha gerada: {} (troque ou defina APP_ADMIN_SENHA)", email, senhaFinal);
        } else {
            log.info("Usuário inicial criado: {}", email);
        }
    }

    private String gerarSenha() {
        byte[] bytes = new byte[9];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
