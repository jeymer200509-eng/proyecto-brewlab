package com.brewlab.features.equipment;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.WaterProfile;
import com.brewlab.domain.validation.FieldErrors;
import com.brewlab.domain.validation.RecipeValidators;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Controlador MVC para Equipos y Perfiles de Agua (/app/equipos). */
public final class EquipmentWaterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        renderIndex(req, resp);
    }

    private void renderIndex(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);

        List<Equipment> equipmentList = container.getEquipmentRepository().findAllByUser(user.getId());
        List<WaterProfile> waterProfiles = container.getWaterProfileRepository().findAllByUser(user.getId());

        String editEqId = WebSupport.trimOrNull(req.getParameter("editEquipmentId"));
        if (editEqId != null && req.getAttribute("editEquipment") == null) {
            req.setAttribute("editEquipment", container.getEquipmentRepository().findById(user.getId(), editEqId));
        }

        String editWaterId = WebSupport.trimOrNull(req.getParameter("editWaterId"));
        if (editWaterId != null && req.getAttribute("editWater") == null) {
            req.setAttribute("editWater", container.getWaterProfileRepository().findById(user.getId(), editWaterId));
        }

        req.setAttribute("equipmentList", equipmentList);
        req.setAttribute("waterProfiles", waterProfiles);
        WebSupport.render(req, resp, "equipos", "Equipos y Perfiles de Agua — BrewLab", "equipment/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = WebSupport.trimOrNull(req.getParameter("action"));
        if ("saveEquipment".equals(action)) {
            handleSaveEquipment(req, resp);
        } else if ("deleteEquipment".equals(action)) {
            handleDeleteEquipment(req, resp);
        } else if ("saveWater".equals(action)) {
            handleSaveWater(req, resp);
        } else if ("deleteWater".equals(action)) {
            handleDeleteWater(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/app/equipos");
        }
    }

    private void handleSaveEquipment(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String id = WebSupport.trimOrNull(req.getParameter("equipmentId"));
        Equipment existing = id != null ? container.getEquipmentRepository().findById(user.getId(), id) : null;

        Equipment candidate = new Equipment(
                existing != null ? existing.getId() : Ids.newId(),
                user.getId(),
                WebSupport.trimOrNull(req.getParameter("type")),
                WebSupport.trimOrNull(req.getParameter("brand")),
                WebSupport.trimOrNull(req.getParameter("model")),
                WebSupport.trimOrNull(req.getParameter("name")),
                WebSupport.trimOrNull(req.getParameter("settingsScale")),
                WebSupport.trimOrNull(req.getParameter("notes")),
                existing != null && existing.isDemo(),
                existing != null ? existing.getCreatedAt() : now,
                now
        );

        FieldErrors errors = RecipeValidators.validateEquipment(candidate);
        if (!errors.isEmpty()) {
            req.setAttribute("eqErrors", errors.asMap());
            req.setAttribute("editEquipment", candidate);
            renderIndex(req, resp);
            return;
        }

        if (existing == null) {
            container.getEquipmentRepository().create(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/equipos", "success", "Equipo registrado.");
        } else {
            container.getEquipmentRepository().update(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/equipos", "success", "Equipo actualizado.");
        }
    }

    private void handleDeleteEquipment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String id = WebSupport.trimOrNull(req.getParameter("equipmentId"));
        if (id != null) {
            container.getEquipmentRepository().delete(user.getId(), id);
        }
        WebSupport.redirectWithFlash(req, resp, "/app/equipos", "info", "Equipo eliminado.");
    }

    private void handleSaveWater(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String id = WebSupport.trimOrNull(req.getParameter("waterId"));
        WaterProfile existing = id != null ? container.getWaterProfileRepository().findById(user.getId(), id) : null;

        FieldErrors errors = new FieldErrors();
        Double tds = parseDouble(req, "tdsPpm", errors, "TDS inválido.");
        Double gh = parseDouble(req, "hardnessGh", errors, "Dureza inválida.");
        Double kh = parseDouble(req, "alkalinityKh", errors, "Alcalinidad inválida.");
        LocalDate measuredAt = parseDate(req, "measuredAt", errors, "Fecha de medición inválida.");

        WaterProfile candidate = new WaterProfile(
                existing != null ? existing.getId() : Ids.newId(),
                user.getId(),
                WebSupport.trimOrNull(req.getParameter("name")),
                WebSupport.trimOrNull(req.getParameter("source")),
                tds,
                gh,
                kh,
                WebSupport.trimOrNull(req.getParameter("minerals")),
                measuredAt,
                WebSupport.trimOrNull(req.getParameter("notes")),
                existing != null && existing.isDemo(),
                existing != null ? existing.getCreatedAt() : now,
                now
        );

        FieldErrors domainErrors = RecipeValidators.validateWaterProfile(candidate);
        for (Map.Entry<String, String> entry : domainErrors.asMap().entrySet()) {
            errors.add(entry.getKey(), entry.getValue());
        }

        if (!errors.isEmpty()) {
            req.setAttribute("waterErrors", errors.asMap());
            req.setAttribute("editWater", candidate);
            renderIndex(req, resp);
            return;
        }

        if (existing == null) {
            container.getWaterProfileRepository().create(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/equipos", "success", "Perfil de agua registrado.");
        } else {
            container.getWaterProfileRepository().update(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/equipos", "success", "Perfil de agua actualizado.");
        }
    }

    private void handleDeleteWater(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String id = WebSupport.trimOrNull(req.getParameter("waterId"));
        if (id != null) {
            container.getWaterProfileRepository().delete(user.getId(), id);
        }
        WebSupport.redirectWithFlash(req, resp, "/app/equipos", "info", "Perfil de agua eliminado.");
    }

    private static Double parseDouble(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseDoubleOrNull(req.getParameter(name));
        } catch (NumberFormatException e) {
            errors.add(name, msg);
            return null;
        }
    }

    private static LocalDate parseDate(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseDateOrNull(req.getParameter(name));
        } catch (RuntimeException e) {
            errors.add(name, msg);
            return null;
        }
    }
}
