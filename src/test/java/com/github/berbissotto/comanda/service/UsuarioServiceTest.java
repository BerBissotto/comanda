package com.github.berbissotto.comanda.service;

import com.github.berbissotto.comanda.dto.UsuarioForm;
import com.github.berbissotto.comanda.model.Role;
import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.repository.UsuarioRepository;
import com.github.berbissotto.comanda.tenant.TenantProvisioner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private PasswordEncoder codificador;
    private TenantProvisioner provisionador;
    private UsuarioService servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        codificador = new BCryptPasswordEncoder();
        provisionador = mock(TenantProvisioner.class);
        servico = new UsuarioService(repositorio, codificador, provisionador);
    }

    private static UsuarioForm form(Long id, String nome, String email, String senha, boolean ativo) {
        UsuarioForm form = new UsuarioForm();
        form.setId(id);
        form.setNome(nome);
        form.setEmail(email);
        form.setSenha(senha);
        form.setAtivo(ativo);
        return form;
    }

    private static Usuario usuario(Long id, String email, Role role, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Empresa " + id);
        usuario.setEmail(email);
        usuario.setSenha("hash-antigo");
        usuario.setRole(role);
        usuario.setAtivo(ativo);
        return usuario;
    }

    private String mensagem(Runnable acao) {
        return assertThrows(IllegalArgumentException.class, acao::run).getMessage();
    }

    // ---------- cadastro novo ----------

    @Test
    void cadastroNovoCriptografaASenhaEViraUsuarioComum() {
        Usuario salvo = servico.salvar(form(null, "  Padaria Central ", " contato@padaria.com ", "senha-forte-1", true), 1L);

        assertEquals("Padaria Central", salvo.getNome());
        assertEquals("contato@padaria.com", salvo.getEmail());
        assertEquals(Role.USER, salvo.getRole(), "a tela nunca cria ADMIN");
        assertTrue(salvo.isAtivo());
        assertNotEquals("senha-forte-1", salvo.getSenha());
        assertTrue(codificador.matches("senha-forte-1", salvo.getSenha()));
        verify(repositorio).save(salvo);
    }

    @Test
    void cadastroNovoPodeNascerInativo() {
        Usuario salvo = servico.salvar(form(null, "Loja", "loja@x.com", "senha-forte-1", false), 1L);
        assertFalse(salvo.isAtivo());
    }

    @Test
    void cadastroNovoExigeTodosOsCampos() {
        assertEquals("Informe a senha", mensagem(() -> servico.salvar(form(null, "Loja", "a@x.com", "", true), 1L)));
        assertEquals("Informe a senha", mensagem(() -> servico.salvar(form(null, "Loja", "a@x.com", "        ", true), 1L)));
        assertEquals("Informe o nome da empresa", mensagem(() -> servico.salvar(form(null, "  ", "a@x.com", "senha-forte-1", true), 1L)));
        assertEquals("Informe o e-mail", mensagem(() -> servico.salvar(form(null, "Loja", " ", "senha-forte-1", true), 1L)));
        verify(repositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void senhaCurtaOuLongaDemaisERecusada() {
        assertEquals("A senha precisa ter pelo menos 8 caracteres",
                mensagem(() -> servico.salvar(form(null, "Loja", "a@x.com", "curta", true), 1L)));
        assertEquals("A senha é longa demais (máximo de 72 caracteres)",
                mensagem(() -> servico.salvar(form(null, "Loja", "a@x.com", "x".repeat(73), true), 1L)));
    }

    @Test
    void emailJaCadastradoERecusadoSemDistinguirMaiusculas() {
        when(repositorio.existsByEmailIgnoreCase("A@x.com")).thenReturn(true);
        assertEquals("Já existe um usuário com este e-mail",
                mensagem(() -> servico.salvar(form(null, "Loja", "A@x.com", "senha-forte-1", true), 1L)));
        verify(repositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // ---------- edição ----------

    @Test
    void edicaoComSenhaEmBrancoMantemASenhaAtual() {
        Usuario existente = usuario(5L, "a@x.com", Role.USER, true);
        when(repositorio.findById(5L)).thenReturn(Optional.of(existente));

        Usuario salvo = servico.salvar(form(5L, "Novo nome", "novo@x.com", "", true), 1L);

        assertEquals("hash-antigo", salvo.getSenha());
        assertEquals("Novo nome", salvo.getNome());
        assertEquals("novo@x.com", salvo.getEmail());
        assertEquals(Role.USER, salvo.getRole());
    }

    @Test
    void edicaoComSenhaNovaTrocaOHash() {
        Usuario existente = usuario(5L, "a@x.com", Role.USER, true);
        when(repositorio.findById(5L)).thenReturn(Optional.of(existente));

        Usuario salvo = servico.salvar(form(5L, "Empresa 5", "a@x.com", "outra-senha-2", true), 1L);

        assertTrue(codificador.matches("outra-senha-2", salvo.getSenha()));
    }

    @Test
    void edicaoNaoPodeTomarOEmailDeOutroUsuario() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(usuario(5L, "a@x.com", Role.USER, true)));
        when(repositorio.existsByEmailIgnoreCaseAndIdNot("b@x.com", 5L)).thenReturn(true);

        assertEquals("Já existe um usuário com este e-mail",
                mensagem(() -> servico.salvar(form(5L, "Empresa 5", "b@x.com", "", true), 1L)));
    }

    @Test
    void usuarioInexistenteNaEdicao() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());
        assertEquals("Usuário não encontrado", mensagem(() -> servico.salvar(form(99L, "X", "x@x.com", "", true), 1L)));
    }

    // ---------- situação ativo/inativo ----------

    @Test
    void inativarOutroUsuarioFunciona() {
        when(repositorio.findById(8L)).thenReturn(Optional.of(usuario(8L, "b@x.com", Role.USER, true)));

        Usuario salvo = servico.salvar(form(8L, "Empresa 8", "b@x.com", "", false), 1L);

        assertFalse(salvo.isAtivo());
        verify(repositorio).save(salvo);
    }

    @Test
    void reativarUsuarioFunciona() {
        when(repositorio.findById(8L)).thenReturn(Optional.of(usuario(8L, "b@x.com", Role.USER, false)));

        Usuario salvo = servico.salvar(form(8L, "Empresa 8", "b@x.com", "", true), 1L);

        assertTrue(salvo.isAtivo());
    }

    @Test
    void administradorNaoPodeInativarOProprioUsuario() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(usuario(5L, "adm@x.com", Role.ADMIN, true)));

        assertEquals("Você não pode inativar o seu próprio usuário",
                mensagem(() -> servico.salvar(form(5L, "Empresa 5", "adm@x.com", "", false), 5L)));
        verify(repositorio, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nuncaInativaOUltimoAdministradorAtivo() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario(7L, "adm@x.com", Role.ADMIN, true)));
        when(repositorio.countByRoleAndAtivoTrue(Role.ADMIN)).thenReturn(1L);

        assertEquals("É preciso manter pelo menos um administrador ativo",
                mensagem(() -> servico.salvar(form(7L, "Empresa 7", "adm@x.com", "", false), 9L)));
    }

    @Test
    void inativaAdministradorQuandoHaOutroAtivo() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(usuario(7L, "adm@x.com", Role.ADMIN, true)));
        when(repositorio.countByRoleAndAtivoTrue(Role.ADMIN)).thenReturn(2L);

        Usuario salvo = servico.salvar(form(7L, "Empresa 7", "adm@x.com", "", false), 9L);

        assertFalse(salvo.isAtivo());
    }

    // ---------- acesso à tela ----------

    @Test
    void soAdministradorAtivoPassaNaConferenciaDoBanco() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(usuario(1L, "adm@x.com", Role.ADMIN, true)));
        when(repositorio.findById(2L)).thenReturn(Optional.of(usuario(2L, "u@x.com", Role.USER, true)));
        when(repositorio.findById(3L)).thenReturn(Optional.of(usuario(3L, "adm2@x.com", Role.ADMIN, false)));
        when(repositorio.findById(4L)).thenReturn(Optional.empty());

        servico.exigirAdministradorAtivo(1L);
        assertThrows(AccessDeniedException.class, () -> servico.exigirAdministradorAtivo(2L));
        assertThrows(AccessDeniedException.class, () -> servico.exigirAdministradorAtivo(3L));
        assertThrows(AccessDeniedException.class, () -> servico.exigirAdministradorAtivo(4L));
        assertThrows(AccessDeniedException.class, () -> servico.exigirAdministradorAtivo(null));
    }

    @Test
    void usuarioLogadoInativoFicaDesabilitadoENoPerfilCerto() {
        UsuarioLogado inativo = new UsuarioLogado(usuario(3L, "i@x.com", Role.USER, false));
        UsuarioLogado admin = new UsuarioLogado(usuario(1L, "adm@x.com", Role.ADMIN, true));

        assertFalse(inativo.isEnabled(), "inativo não consegue fazer login");
        assertTrue(admin.isEnabled());
        assertEquals(1L, admin.getId());
        assertTrue(admin.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        assertTrue(inativo.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
    }

    @Test
    void usuarioNovoGanhaEmpresaNaMesmaTransacao() {
        when(repositorio.save(org.mockito.ArgumentMatchers.any(Usuario.class))).thenAnswer(chamada -> {
            Usuario gravado = chamada.getArgument(0);
            gravado.setId(42L);
            return gravado;
        });

        servico.salvar(form(null, "Loja", "loja@x.com", "senha-forte-1", true), 1L);

        verify(provisionador).provisionar(42L);
    }

    @Test
    void edicaoNaoCriaEmpresa() {
        when(repositorio.findById(5L)).thenReturn(Optional.of(usuario(5L, "a@x.com", Role.USER, true)));

        servico.salvar(form(5L, "Novo nome", "a@x.com", "", true), 1L);

        verify(provisionador, never()).provisionar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void falhaAoCriarEmpresaViraMensagemParaQuemCadastra() {
        org.mockito.Mockito.doThrow(new IllegalStateException("Não foi possível criar o espaço de dados"))
                .when(provisionador).provisionar(org.mockito.ArgumentMatchers.any());

        assertEquals("Não foi possível criar o espaço de dados",
                mensagem(() -> servico.salvar(form(null, "Loja", "loja@x.com", "senha-forte-1", true), 1L)));
    }
}
