package com.brewlab.security;

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
 * Filtro de lista blanca y cabeceras de seguridad. Impide que archivos de código fuente, scripts
 * o configuración en la raíz del proyecto sean servidos por Tomcat.
 */
public final class PublicAccessFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }
        if (!isAllowedPath(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean isAllowedPath(String path) {
        if ("/".equals(path) || "/index.jsp".equals(path) || "/manifest.webmanifest".equals(path)) {
            return true;
        }
        if (path.startsWith("/assets/") && !path.contains("..")) {
            return true;
        }
        if (path.startsWith("/auth/") || path.startsWith("/app/")) {
            return true;
        }
        return false;
    }

    @Override
    public void destroy() {
    }
}
