package com.brewlab.features.auth;

import com.brewlab.app.AppContainer;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.User;
import com.brewlab.domain.validation.FieldErrors;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.regex.Pattern;

/** Controlador MVC de autenticación JWT (login, registro y salida). */
public final class AuthServlet extends HttpServlet {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (WebSupport.currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/app/inicio");
            return;
        }
        String path = req.getServletPath();
        if ("/auth/registro".equals(path)) {
            WebSupport.render(req, resp, "auth", "Crear cuenta — BrewLab", "auth/register.jsp");
        } else {
            WebSupport.render(req, resp, "auth", "Iniciar sesión — BrewLab", "auth/login.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/auth/salir".equals(path)) {
            handleLogout(req, resp);
        } else if ("/auth/registro".equals(path)) {
            handleRegister(req, resp);
        } else {
            handleLogin(req, resp);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        String email = WebSupport.trimOrNull(req.getParameter("email"));
        String password = req.getParameter("password");

        FieldErrors errors = new FieldErrors();
        if (email == null) {
            errors.add("email", "Introduce tu correo electrónico.");
        }
        if (password == null || password.isEmpty()) {
            errors.add("password", "Introduce tu contraseña.");
        }
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors.asMap());
            req.setAttribute("formEmail", email);
            WebSupport.render(req, resp, "auth", "Iniciar sesión — BrewLab", "auth/login.jsp");
            return;
        }

        User user = container.getUserRepository().findByEmail(email);
        if (user == null || !container.getPasswordHasher().verify(password, user.getPasswordHash())) {
            errors.add("general", "Correo o contraseña incorrectos.");
            req.setAttribute("errors", errors.asMap());
            req.setAttribute("formEmail", email);
            WebSupport.render(req, resp, "auth", "Iniciar sesión — BrewLab", "auth/login.jsp");
            return;
        }

        Instant now = Instant.now(container.getClock());
        container.getUserRepository().updateLastLogin(user.getId(), now);
        String jwt = container.getJwtService().issueToken(user, now);
        WebSupport.setJwtCookie(req, resp, jwt, container.getJwtService().getMaxAgeSeconds());
        WebSupport.redirectWithFlash(req, resp, "/app/inicio", "success",
                "Bienvenido al laboratorio, " + user.getDisplayName() + ".");
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        String displayName = WebSupport.trimOrNull(req.getParameter("displayName"));
        String email = WebSupport.trimOrNull(req.getParameter("email"));
        String password = req.getParameter("password");

        FieldErrors errors = validateRegistration(displayName, email, password, container.getUserRepository());
        if (!errors.isEmpty()) {
            req.setAttribute("errors", errors.asMap());
            req.setAttribute("formDisplayName", displayName);
            req.setAttribute("formEmail", email);
            WebSupport.render(req, resp, "auth", "Crear cuenta — BrewLab", "auth/register.jsp");
            return;
        }

        Instant now = Instant.now(container.getClock());
        String passwordHash = container.getPasswordHasher().hash(password);
        User created = container.getUserRepository().create(
                Ids.newId(), email, displayName, passwordHash, now);
        container.getUserRepository().updateLastLogin(created.getId(), now);

        String jwt = container.getJwtService().issueToken(created, now);
        WebSupport.setJwtCookie(req, resp, jwt, container.getJwtService().getMaxAgeSeconds());
        WebSupport.redirectWithFlash(req, resp, "/app/inicio", "success",
                "Cuenta creada. Sesión autenticada mediante JWT.");
    }

    public static FieldErrors validateRegistration(String displayName, String email,
                                                   String password, UserRepository userRepo) {
        FieldErrors errors = new FieldErrors();
        if (displayName == null || displayName.length() < 2) {
            errors.add("displayName", "Indica un nombre o alias de al menos 2 caracteres.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("email", "Introduce un correo electrónico válido.");
        } else if (userRepo != null && userRepo.findByEmail(email) != null) {
            errors.add("email", "Ya existe una cuenta registrada con este correo.");
        }
        if (password == null || password.length() < 8) {
            errors.add("password", "La contraseña debe tener al menos 8 caracteres.");
        }
        return errors;
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User current = WebSupport.currentUser(req);
        if (current != null) {
            container.getUserRepository().incrementTokenVersion(
                    current.getId(), Instant.now(container.getClock()));
        }
        WebSupport.clearJwtCookie(req, resp);
        WebSupport.redirectWithFlash(req, resp, "/auth/login", "info", "Sesión cerrada correctamente.");
    }
}
