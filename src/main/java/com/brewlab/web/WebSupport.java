package com.brewlab.web;

import com.brewlab.domain.model.User;
import com.brewlab.security.CsrfTokens;
import com.brewlab.security.JwtService;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Utilidades comunes para controladores MVC (Servlets + JSP). */
public final class WebSupport {
    public static final String ATTR_CURRENT_USER = "currentUser";
    public static final String ATTR_CSRF_TOKEN = "csrfToken";
    public static final String COOKIE_FLASH = "brewlab_flash";

    private WebSupport() {
    }

    public static User currentUser(HttpServletRequest req) {
        return (User) req.getAttribute(ATTR_CURRENT_USER);
    }

    public static void render(HttpServletRequest req, HttpServletResponse resp,
                              String activeNav, String pageTitle, String viewJsp)
            throws ServletException, IOException {
        req.setAttribute("activeNav", activeNav);
        req.setAttribute("pageTitle", pageTitle);
        consumeFlash(req, resp);
        resp.setContentType("text/html; charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/" + viewJsp).forward(req, resp);
    }

    public static void redirectWithFlash(HttpServletRequest req, HttpServletResponse resp,
                                         String path, String type, String message) throws IOException {
        try {
            String payload = type + "|" + URLEncoder.encode(message, StandardCharsets.UTF_8.name());
            setCookie(req, resp, COOKIE_FLASH, payload, 60, true);
        } catch (Exception ignored) {
            // Si falla la codificación, la redirección continúa sin flash
        }
        resp.sendRedirect(req.getContextPath() + path);
    }

    public static void consumeFlash(HttpServletRequest req, HttpServletResponse resp) {
        String raw = getCookieValue(req, COOKIE_FLASH);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        clearCookie(req, resp, COOKIE_FLASH);
        int sep = raw.indexOf('|');
        if (sep <= 0) {
            return;
        }
        try {
            String type = raw.substring(0, sep);
            String msg = java.net.URLDecoder.decode(raw.substring(sep + 1), StandardCharsets.UTF_8.name());
            req.setAttribute("flashType", type);
            req.setAttribute("flashMessage", msg);
        } catch (Exception ignored) {
        }
    }

    public static String getCookieValue(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    public static void setJwtCookie(HttpServletRequest req, HttpServletResponse resp,
                                    String jwt, int maxAgeSeconds) {
        setCookie(req, resp, JwtService.COOKIE_NAME, jwt, maxAgeSeconds, true);
    }

    public static void clearJwtCookie(HttpServletRequest req, HttpServletResponse resp) {
        clearCookie(req, resp, JwtService.COOKIE_NAME);
    }

    public static String ensureCsrfCookie(HttpServletRequest req, HttpServletResponse resp) {
        String token = getCookieValue(req, CsrfTokens.COOKIE_NAME);
        if (token == null || token.length() < 16) {
            token = CsrfTokens.generate();
            setCookie(req, resp, CsrfTokens.COOKIE_NAME, token, 12 * 3600, true);
        }
        req.setAttribute(ATTR_CSRF_TOKEN, token);
        return token;
    }

    public static void setCookie(HttpServletRequest req, HttpServletResponse resp,
                                 String name, String value, int maxAgeSeconds, boolean httpOnly) {
        String path = cookiePath(req);
        StringBuilder header = new StringBuilder();
        header.append(name).append('=').append(value)
                .append("; Path=").append(path)
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; SameSite=Strict");
        if (httpOnly) {
            header.append("; HttpOnly");
        }
        if (req.isSecure()) {
            header.append("; Secure");
        }
        resp.addHeader("Set-Cookie", header.toString());
    }

    public static void clearCookie(HttpServletRequest req, HttpServletResponse resp, String name) {
        setCookie(req, resp, name, "", 0, true);
    }

    private static String cookiePath(HttpServletRequest req) {
        String ctx = req.getContextPath();
        if (ctx == null || ctx.isEmpty()) {
            return "/";
        }
        return ctx.replace(" ", "%20");
    }

    /** Devuelve null si el parámetro está vacío (nunca convierte ausencia en texto vacío o "0"). */
    public static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Convierte texto numérico opcional a {@link Double}. Si está vacío devuelve {@code null},
     * nunca {@code 0.0}. Acepta coma o punto decimal.
     */
    public static Double parseDoubleOrNull(String raw) throws NumberFormatException {
        String t = trimOrNull(raw);
        if (t == null) {
            return null;
        }
        return Double.valueOf(t.replace(',', '.'));
    }

    public static Integer parseIntOrNull(String raw) throws NumberFormatException {
        String t = trimOrNull(raw);
        if (t == null) {
            return null;
        }
        return Integer.valueOf(t);
    }

    public static LocalDate parseDateOrNull(String raw) throws DateTimeParseException {
        String t = trimOrNull(raw);
        if (t == null) {
            return null;
        }
        return LocalDate.parse(t);
    }
}
