package com.brewlab.features.backup;

import com.brewlab.app.AppContainer;
import com.brewlab.data.backup.BackupService;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.UserSettings;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Controlador MVC de Respaldo JSON/CSV, Restauración con Prevalidación y Semilla [DEMO] (/app/respaldo).
 */
public final class BackupServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String action = WebSupport.trimOrNull(req.getParameter("action"));

        if ("exportJson".equals(action)) {
            String json = container.getBackupService().exportJson(
                    user.getId(), container.getSchemaVersion(), Instant.now(container.getClock())
            );
            resp.setContentType("application/json; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"brewlab-backup-v"
                    + container.getSchemaVersion() + ".json\"");
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            return;
        }

        if ("exportCsv".equals(action)) {
            String table = WebSupport.trimOrNull(req.getParameter("table"));
            if (table == null) {
                table = "brew_sessions";
            }
            String csv = container.getBackupService().exportCsv(user.getId(), table);
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"brewlab-" + table + ".csv\"");
            resp.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/app/ajustes");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String action = WebSupport.trimOrNull(req.getParameter("action"));

        if ("seedDemo".equals(action)) {
            container.getDemoDataSeeder().seed(
                    user.getId(),
                    LocalDate.now(container.getClock()),
                    Instant.now(container.getClock())
            );
            WebSupport.redirectWithFlash(req, resp, "/app/ajustes", "success",
                    "Datos de demostración marcados explícitamente como [DEMO] cargados correctamente.");
            return;
        }

        if ("clearAll".equals(action)) {
            container.getBackupService().clearUserData(user.getId());
            WebSupport.redirectWithFlash(req, resp, "/app/ajustes", "info",
                    "Se han vaciado todos tus registros del laboratorio.");
            return;
        }

        if ("previewImport".equals(action)) {
            String rawJson = req.getParameter("jsonPayload");
            BackupService.ImportPreview preview = container.getBackupService()
                    .prevalidate(rawJson, container.getSchemaVersion());

            UserSettings settings = container.getUserSettingsRepository().findOrDefault(user.getId());
            req.setAttribute("settings", settings);
            req.setAttribute("lastSavedFormatted",
                    Formats.dateTime(settings.getUpdatedAt(), container.getConfig().getTimezone()));
            req.setAttribute("dbEngineLabel", container.getManagedDataSource().getEngineLabel());
            req.setAttribute("dbUrlSummary", container.getManagedDataSource().getJdbcUrlSummary());
            req.setAttribute("dbUsingFallback", Boolean.valueOf(container.getManagedDataSource().isUsingFallback()));
            req.setAttribute("schemaVersion", Integer.valueOf(container.getSchemaVersion()));
            req.setAttribute("backupSectionEnabled", Boolean.TRUE);
            req.setAttribute("previewJsonPayload", rawJson);
            req.setAttribute("importPreview", preview);

            WebSupport.render(req, resp, "ajustes", "Configuración y Respaldo — BrewLab", "settings/index.jsp");
            return;
        }

        if ("confirmImport".equals(action)) {
            if (!"true".equalsIgnoreCase(req.getParameter("confirmReplace"))) {
                WebSupport.redirectWithFlash(req, resp, "/app/ajustes", "warn",
                        "Debes marcar la casilla de confirmación antes de restaurar el respaldo.");
                return;
            }
            String rawJson = req.getParameter("jsonPayload");
            try {
                BackupService.ImportPreview restored = container.getBackupService()
                        .restoreJson(user.getId(), rawJson, container.getSchemaVersion());
                WebSupport.redirectWithFlash(req, resp, "/app/ajustes", "success",
                        "Respaldo restaurado correctamente (esquema v" + restored.getSchemaVersion()
                                + ": " + restored.getCoffeeLotsCount() + " lotes, "
                                + restored.getSessionsCount() + " sesiones, "
                                + restored.getAssessmentsCount() + " evaluaciones).");
            } catch (IllegalArgumentException e) {
                WebSupport.redirectWithFlash(req, resp, "/app/ajustes", "error",
                        "No se restauró el respaldo (tus datos actuales permanecen intactos): " + e.getMessage());
            }
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/app/ajustes");
    }
}
