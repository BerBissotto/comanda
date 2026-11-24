package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.repository.UsuarioRepository;
import com.github.berbissotto.comanda.tenant.TenantProvisioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoginAttemptService tentativas;

    @Autowired
    private TenantProvisioner provisionador;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Checa o bloqueio ANTES de consultar o banco: vale também para e-mails inexistentes
        if (tentativas.estaBloqueado(email)) {
            throw new LockedException("Muitas tentativas de login");
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        // Usuário criado direto no banco (SQL) ainda não tem espaço de dados: cria no 1º login
        if (usuario.isAtivo()) {
            provisionador.garantir(usuario.getId());
        }
        // Perfil (ADMIN/USER) e situação (ativo/inativo) vêm do banco; inativo não consegue entrar
        return new UsuarioLogado(usuario);
    }
}
