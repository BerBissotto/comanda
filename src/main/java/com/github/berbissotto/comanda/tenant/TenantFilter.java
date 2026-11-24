package com.github.berbissotto.comanda.tenant;

import com.github.berbissotto.comanda.service.UsuarioLogado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Define a empresa da requisição a partir do USUÁRIO AUTENTICADO (id vindo da sessão segura do
 * servidor), nunca de algo que o navegador envie. Roda dentro da cadeia do Spring Security,
 * depois da autorização, e sempre limpa a empresa no fim (a thread volta ao pool de threads).
 *
 * <p>A tela de Usuários (/usuarios) é do contexto central: lê e grava a tabela usuario em public
 * e, ao cadastrar, cria a empresa nova. Ela não recebe empresa. Se alguma rota ficar no contexto
 * errado, o banco recusa o acesso (falha fechada): nunca mistura dados.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        TenantContext.limpar();
        try {
            if (!ehContextoCentral(request)) {
                Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
                if (autenticacao != null && autenticacao.isAuthenticated()
                        && autenticacao.getPrincipal() instanceof UsuarioLogado logado) {
                    TenantContext.definir(logado.schema());
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.limpar();
        }
    }

    static boolean ehContextoCentral(HttpServletRequest request) {
        String caminho = request.getServletPath();
        return caminho.equals("/usuarios") || caminho.startsWith("/usuarios/");
    }
}
