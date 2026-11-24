package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.dto.UsuarioForm;
import com.github.berbissotto.comanda.model.Role;
import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.repository.UsuarioRepository;
import com.github.berbissotto.comanda.tenant.TenantProvisioner;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    static final int SENHA_MINIMA = 8;
    // limite do BCrypt: ele só lê os primeiros 72 bytes da senha
    static final int SENHA_MAXIMA_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantProvisioner provisionador;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          TenantProvisioner provisionador) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.provisionador = provisionador;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll(Sort.by(Sort.Order.asc("nome").ignoreCase()));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    /**
     * Confere no banco, a cada acesso à tela de Usuários, se quem está logado ainda é um ADMIN
     * ativo. Sem isso, um administrador rebaixado ou inativado continuaria mexendo em usuários
     * até a sessão dele expirar.
     */
    @Transactional(readOnly = true)
    public void exigirAdministradorAtivo(Long idLogado) {
        boolean permitido = idLogado != null && usuarioRepository.findById(idLogado)
                .filter(usuario -> usuario.isAtivo() && usuario.getRole() == Role.ADMIN)
                .isPresent();
        if (!permitido) {
            throw new AccessDeniedException("Acesso restrito a administradores");
        }
    }

    /**
     * Cria (form sem id) ou altera (form com id) um usuário. Lança IllegalArgumentException com a
     * mensagem que deve aparecer para quem está cadastrando.
     */
    @Transactional
    public Usuario salvar(UsuarioForm form, Long idLogado) {
        boolean novo = form.getId() == null;
        String nome = form.getNome() == null ? "" : form.getNome().trim();
        String email = form.getEmail() == null ? "" : form.getEmail().trim();
        boolean ativo = Boolean.TRUE.equals(form.getAtivo());
        String senha = form.getSenha();
        boolean informouSenha = senha != null && !senha.isBlank();

        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Informe o nome da empresa");
        }
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail");
        }
        if (novo && !informouSenha) {
            throw new IllegalArgumentException("Informe a senha");
        }
        if (informouSenha) {
            validarSenha(senha);
        }

        Usuario usuario;
        if (novo) {
            usuario = new Usuario();
            usuario.setRole(Role.USER);
        } else {
            usuario = usuarioRepository.findById(form.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        }

        boolean emailEmUso = novo
                ? usuarioRepository.existsByEmailIgnoreCase(email)
                : usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, usuario.getId());
        if (emailEmUso) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail");
        }

        if (!novo && !ativo) {
            if (usuario.getId().equals(idLogado)) {
                throw new IllegalArgumentException("Você não pode inativar o seu próprio usuário");
            }
            // proteção extra: nunca deixar o sistema sem nenhum administrador ativo
            if (usuario.isAtivo() && usuario.getRole() == Role.ADMIN
                    && usuarioRepository.countByRoleAndAtivoTrue(Role.ADMIN) <= 1) {
                throw new IllegalArgumentException("É preciso manter pelo menos um administrador ativo");
            }
        }

        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setAtivo(ativo);
        if (informouSenha) {
            usuario.setSenha(passwordEncoder.encode(senha));
        }

        usuarioRepository.save(usuario);
        if (novo) {
            // Cada empresa tem o seu próprio espaço de dados. Mesma transação do cadastro: se a
            // criação falhar, o usuário também não é gravado.
            try {
                provisionador.provisionar(usuario.getId());
            } catch (IllegalStateException erro) {
                throw new IllegalArgumentException(erro.getMessage(), erro);
            }
        }
        return usuario;
    }

    private void validarSenha(String senha) {
        if (senha.length() < SENHA_MINIMA) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMA_BYTES) {
            throw new IllegalArgumentException("A senha é longa demais (máximo de 72 caracteres)");
        }
    }
}
