package com.brewlab.features.settings;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.UserSettings;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

/** Controlador de Configuración del laboratorio y preferencias de usuario. */
public final class SettingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        UserSettings settings = container.getUserSettingsRepository().findOrDefault(user.getId());

        req.setAttribute("settings", settings);
        req.setAttribute("lastSavedFormatted",
                Formats.dateTime(settings.getUpdatedAt(), container.getConfig().getTimezone()));
        req.setAttribute("dbEngineLabel", container.getManagedDataSource().getEngineLabel());
        req.setAttribute("dbUrlSummary", container.getManagedDataSource().getJdbcUrlSummary());
        req.setAttribute("dbUsingFallback", Boolean.valueOf(container.getManagedDataSource().isUsingFallback()));
        req.setAttribute("schemaVersion", Integer.valueOf(container.getSchemaVersion()));
        req.setAttribute("backupSectionEnabled", Boolean.TRUE);

        WebSupport.render(req, resp, "ajustes", "Configuración — BrewLab", "settings/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);

        boolean soundEnabled = "on".equalsIgnoreCase(req.getParameter("soundEnabled"))
                || "true".equalsIgnoreCase(req.getParameter("soundEnabled"));
        boolean keepScreenAwake = "on".equalsIgnoreCase(req.getParameter("keepScreenAwake"))
                || "true".equalsIgnoreCase(req.getParameter("keepScreenAwake"));
        UserSettings.TimerTheme theme = UserSettings.TimerTheme.fromCode(req.getParameter("timerTheme"));
        if (theme == null) {
            theme = UserSettings.TimerTheme.AUTO;
        }

        Instant now = Instant.now(container.getClock());
        UserSettings updated = new UserSettings(user.getId(), soundEnabled, keepScreenAwake, theme, now);
        container.getUserSettingsRepository().save(updated, now);

        WebSupport.redirectWithFlash(req, resp, "/app/ajustes", "success",
                "Preferencias del laboratorio guardadas.");
    }
}
