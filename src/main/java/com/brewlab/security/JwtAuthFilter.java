package com.brewlab.security;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.model.User;
import com.brewlab.web.WebSupport;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro de autenticación JWT y protección CSRF para rutas {@code /app/*} y {@code /auth/*}.
 */
public final class JwtAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        AppContainer container = AppContainer.from(req.getServletContext());
        WebSupport.ensureCsrfCookie(req, resp);

        String path = req.getRequestURI().substring(req.getContextPath().length());
        User authenticatedUser = resolveUserFromJwt(req, container);
        if (authenticatedUser != null) {
            req.setAttribute(WebSupport.ATTR_CURRENT_USER, authenticatedUser);
        }

        if (path.startsWith("/app/")) {
            if (authenticatedUser == null) {
                WebSupport.clearJwtCookie(req, resp);
                resp.sendRedirect(req.getContextPath() + "/auth/login");
                return;
            }
        }

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String cookieToken = WebSupport.getCookieValue(req, CsrfTokens.COOKIE_NAME);
            String formToken = req.getParameter(CsrfTokens.FORM_FIELD);
            if (!CsrfTokens.matches(cookieToken, formToken)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Token CSRF inválido o ausente.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static User resolveUserFromJwt(HttpServletRequest req, AppContainer container) {
        String rawJwt = WebSupport.getCookieValue(req, JwtService.COOKIE_NAME);
        if (rawJwt == null) {
            return null;
        }
        JwtService.VerifiedToken verified = container.getJwtService().verify(rawJwt);
        if (verified == null) {
            return null;
        }
        User user = container.getUserRepository().findById(verified.getUserId());
        if (user == null || user.getTokenVersion() != verified.getTokenVersion()) {
            return null;
        }
        req.setAttribute("jwtVerified", verified);
        return user;
    }

    @Override
    public void destroy() {
    }
}
