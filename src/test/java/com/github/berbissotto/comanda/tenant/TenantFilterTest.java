package com.github.berbissotto.comanda.tenant;

import com.github.berbissotto.comanda.model.Role;
import com.github.berbissotto.comanda.model.Usuario;
import com.github.berbissotto.comanda.service.UsuarioLogado;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantFilterTest {

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        TenantContext.limpar();
    }

    private static UsuarioLogado logado(long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail("u" + id + "@x.com");
        usuario.setSenha("hash");
        usuario.setRole(Role.USER);
        usuario.setAtivo(true);
        return new UsuarioLogado(usuario);
    }

    private static void autenticar(UsuarioLogado usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities()));
    }

    private List<String> empresasVistasPor(String caminho) throws Exception {
        List<String> vistas = new ArrayList<>();
        FilterChain cadeia = (requisicao, resposta) -> vistas.add(TenantContext.atual());
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", caminho);
        requisicao.setServletPath(caminho);
        new TenantFilter().doFilter(requisicao, new MockHttpServletResponse(), cadeia);
        return vistas;
    }

    @Test
    void usuarioLogadoAtendidoNaPropriaEmpresa() throws Exception {
        autenticar(logado(7));

        assertEquals(List.of("empresa_7"), empresasVistasPor("/produtos"));
        assertNull(TenantContext.atual());
    }

    @Test
    void empresaDependeSoDoUsuarioLogadoNuncaDeParametro() throws Exception {
        autenticar(logado(7));
        List<String> vistas = new ArrayList<>();
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/produtos");
        requisicao.setServletPath("/produtos");
        requisicao.setParameter("empresa", "empresa_99");
        requisicao.addHeader("X-Empresa", "empresa_99");

        new TenantFilter().doFilter(requisicao, new MockHttpServletResponse(),
                (req, res) -> vistas.add(TenantContext.atual()));

        assertEquals(List.of("empresa_7"), vistas);
    }

    @Test
    void semLoginNaoHaEmpresa() throws Exception {
        assertEquals(java.util.Collections.singletonList(null), empresasVistasPor("/login"));
    }

    @Test
    void anonimoNaoTemEmpresa() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertEquals(java.util.Collections.singletonList(null), empresasVistasPor("/produtos"));
    }

    @Test
    void telaDeUsuariosEhContextoCentralMesmoParaOAdministradorLogado() throws Exception {
        autenticar(logado(1));

        assertEquals(java.util.Collections.singletonList(null), empresasVistasPor("/usuarios"));
        assertEquals(java.util.Collections.singletonList(null), empresasVistasPor("/usuarios/salvar"));
        assertEquals(java.util.Collections.singletonList(null), empresasVistasPor("/usuarios/3/editar"));
    }

    @Test
    void rotaParecidaComUsuariosNaoEscapaDaEmpresa() throws Exception {
        autenticar(logado(1));

        assertEquals(List.of("empresa_1"), empresasVistasPor("/usuariosx"));
    }

    @Test
    void empresaEhLimpaMesmoQuandoARequisicaoFalha() {
        autenticar(logado(7));
        FilterChain cadeia = (requisicao, resposta) -> {
            throw new IllegalStateException("erro na tela");
        };
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/produtos");
        requisicao.setServletPath("/produtos");

        assertThrows(IllegalStateException.class,
                () -> new TenantFilter().doFilter(requisicao, new MockHttpServletResponse(), cadeia));

        assertNull(TenantContext.atual());
    }

    @Test
    void empresaDeUmaRequisicaoNaoVazaParaAProximaNaMesmaThread() throws Exception {
        TenantContext.definir("empresa_1");
        SecurityContextHolder.clearContext();

        assertEquals(java.util.Collections.singletonList(null), empresasVistasPor("/login"));
    }
}
