package com.brewlab.features.coffee;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.Ids;
import com.brewlab.domain.calc.RoastAge;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.User;
import com.brewlab.domain.validation.CoffeeValidators;
import com.brewlab.domain.validation.FieldErrors;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controlador MVC para Biblioteca de Cafés (CoffeeLot) y Lotes de Tostión (RoastBatch). */
public final class CoffeeLibraryServlet extends HttpServlet {

    /** Vista enriquecida de un RoastBatch con su edad calculada respecto a la fecha local actual. */
    public static final class BatchView {
        private final RoastBatch batch;
        private final RoastAge age;

        public BatchView(RoastBatch batch, RoastAge age) {
            this.batch = batch;
            this.age = age;
        }

        public RoastBatch getBatch() {
            return batch;
        }

        public RoastAge getAge() {
            return age;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/app/cafes/lote".equals(path)) {
            renderLotDetail(req, resp);
        } else {
            renderLotList(req, resp);
        }
    }

    private void renderLotList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String q = WebSupport.trimOrNull(req.getParameter("q"));
        boolean showArchived = "true".equalsIgnoreCase(req.getParameter("archived"));

        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), q, showArchived);
        Map<String, List<BatchView>> batchesByLot = new HashMap<>();
        LocalDate today = LocalDate.now(container.getClock());
        for (CoffeeLot lot : lots) {
            List<RoastBatch> rawBatches = container.getRoastBatchRepository()
                    .findByCoffeeLot(user.getId(), lot.getId(), showArchived);
            List<BatchView> views = new ArrayList<>(rawBatches.size());
            for (RoastBatch b : rawBatches) {
                views.add(new BatchView(b, b.ageAt(today)));
            }
            batchesByLot.put(lot.getId(), views);
        }

        String editId = WebSupport.trimOrNull(req.getParameter("editId"));
        if (editId != null) {
            CoffeeLot editLot = container.getCoffeeLotRepository().findById(user.getId(), editId);
            req.setAttribute("editLot", editLot);
        }

        req.setAttribute("lots", lots);
        req.setAttribute("batchesByLot", batchesByLot);
        req.setAttribute("query", q);
        req.setAttribute("showArchived", Boolean.valueOf(showArchived));
        req.setAttribute("todayFormatted", Formats.date(today));
        WebSupport.render(req, resp, "cafes", "Biblioteca de Cafés — BrewLab", "coffee/list.jsp");
    }

    private void renderLotDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String lotId = WebSupport.trimOrNull(req.getParameter("id"));
        CoffeeLot lot = container.getCoffeeLotRepository().findById(user.getId(), lotId);
        if (lot == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/cafes", "error", "Lote de café no encontrado.");
            return;
        }

        boolean showArchived = "true".equalsIgnoreCase(req.getParameter("archived"));
        LocalDate today = LocalDate.now(container.getClock());
        List<RoastBatch> rawBatches = container.getRoastBatchRepository()
                .findByCoffeeLot(user.getId(), lot.getId(), showArchived);
        List<BatchView> batchViews = new ArrayList<>(rawBatches.size());
        for (RoastBatch b : rawBatches) {
            batchViews.add(new BatchView(b, b.ageAt(today)));
        }

        String editBatchId = WebSupport.trimOrNull(req.getParameter("editBatchId"));
        if (editBatchId != null) {
            RoastBatch editBatch = container.getRoastBatchRepository().findById(user.getId(), editBatchId);
            if (editBatch != null && lot.getId().equals(editBatch.getCoffeeLotId())) {
                req.setAttribute("editBatch", editBatch);
            }
        }

        req.setAttribute("lot", lot);
        req.setAttribute("batchViews", batchViews);
        req.setAttribute("showArchived", Boolean.valueOf(showArchived));
        req.setAttribute("todayIso", today.toString());
        req.setAttribute("todayFormatted", Formats.date(today));
        WebSupport.render(req, resp, "cafes", lot.getName() + " — Ficha de Café", "coffee/detail.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = WebSupport.trimOrNull(req.getParameter("action"));
        if ("saveLot".equals(action)) {
            handleSaveLot(req, resp);
        } else if ("archiveLot".equals(action) || "unarchiveLot".equals(action)) {
            handleToggleArchiveLot(req, resp, "archiveLot".equals(action));
        } else if ("saveBatch".equals(action)) {
            handleSaveBatch(req, resp);
        } else if ("archiveBatch".equals(action) || "unarchiveBatch".equals(action)) {
            handleToggleArchiveBatch(req, resp, "archiveBatch".equals(action));
        } else {
            resp.sendRedirect(req.getContextPath() + "/app/cafes");
        }
    }

    private void handleSaveLot(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String id = WebSupport.trimOrNull(req.getParameter("id"));
        CoffeeLot existing = id != null ? container.getCoffeeLotRepository().findById(user.getId(), id) : null;

        FieldErrors parseErrors = new FieldErrors();
        Integer altMin = parseIntField(req, "altitudeMin", parseErrors, "Altitud mínima inválida.");
        Integer altMax = parseIntField(req, "altitudeMax", parseErrors, "Altitud máxima inválida.");
        LocalDate receivedOn = parseDateField(req, "receivedOn", parseErrors, "Fecha de recepción inválida.");
        Double moisture = parseDoubleField(req, "moisturePct", parseErrors, "Humedad inválida.");
        Double density = parseDoubleField(req, "density", parseErrors, "Densidad inválida.");

        CoffeeLot candidate = new CoffeeLot(
                existing != null ? existing.getId() : Ids.newId(),
                user.getId(),
                WebSupport.trimOrNull(req.getParameter("code")),
                WebSupport.trimOrNull(req.getParameter("name")),
                WebSupport.trimOrNull(req.getParameter("country")),
                WebSupport.trimOrNull(req.getParameter("region")),
                WebSupport.trimOrNull(req.getParameter("municipality")),
                WebSupport.trimOrNull(req.getParameter("farm")),
                WebSupport.trimOrNull(req.getParameter("producer")),
                WebSupport.trimOrNull(req.getParameter("species")),
                WebSupport.trimOrNull(req.getParameter("variety")),
                WebSupport.trimOrNull(req.getParameter("process")),
                altMin,
                altMax,
                WebSupport.trimOrNull(req.getParameter("harvest")),
                receivedOn,
                moisture,
                density,
                WebSupport.trimOrNull(req.getParameter("screenSize")),
                WebSupport.trimOrNull(req.getParameter("notes")),
                existing != null && existing.isDemo(),
                existing != null ? existing.getArchivedAt() : null,
                existing != null ? existing.getCreatedAt() : now,
                now
        );

        FieldErrors domainErrors = CoffeeValidators.validateCoffeeLot(candidate);
        for (Map.Entry<String, String> entry : domainErrors.asMap().entrySet()) {
            parseErrors.add(entry.getKey(), entry.getValue());
        }

        if (!parseErrors.isEmpty()) {
            req.setAttribute("lotErrors", parseErrors.asMap());
            req.setAttribute("editLot", candidate);
            renderLotList(req, resp);
            return;
        }

        if (existing == null) {
            container.getCoffeeLotRepository().create(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/cafes/lote?id=" + candidate.getId(),
                    "success", "Lote de café registrado. Ahora puedes asociarle lotes de tostión.");
        } else {
            container.getCoffeeLotRepository().update(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/cafes/lote?id=" + candidate.getId(),
                    "success", "Ficha del lote de café actualizada.");
        }
    }

    private void handleToggleArchiveLot(HttpServletRequest req, HttpServletResponse resp, boolean archive)
            throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String id = WebSupport.trimOrNull(req.getParameter("id"));
        CoffeeLot existing = container.getCoffeeLotRepository().findById(user.getId(), id);
        if (existing == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/cafes", "error", "Lote no encontrado.");
            return;
        }
        Instant now = Instant.now(container.getClock());
        CoffeeLot updated = new CoffeeLot(
                existing.getId(), existing.getUserId(), existing.getCode(), existing.getName(),
                existing.getCountry(), existing.getRegion(), existing.getMunicipality(),
                existing.getFarm(), existing.getProducer(), existing.getSpecies(), existing.getVariety(),
                existing.getProcess(), existing.getAltitudeMin(), existing.getAltitudeMax(),
                existing.getHarvest(), existing.getReceivedOn(), existing.getMoisturePct(),
                existing.getDensity(), existing.getScreenSize(), existing.getNotes(),
                existing.isDemo(), archive ? now : null, existing.getCreatedAt(), now
        );
        container.getCoffeeLotRepository().update(updated);
        WebSupport.redirectWithFlash(req, resp, "/app/cafes?archived=true", "info",
                archive ? "Lote de café archivado." : "Lote de café restaurado.");
    }

    private void handleSaveBatch(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String coffeeLotId = WebSupport.trimOrNull(req.getParameter("coffeeLotId"));
        CoffeeLot lot = container.getCoffeeLotRepository().findById(user.getId(), coffeeLotId);
        if (lot == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/cafes", "error", "Lote de café no encontrado.");
            return;
        }

        Instant now = Instant.now(container.getClock());
        String batchId = WebSupport.trimOrNull(req.getParameter("batchId"));
        RoastBatch existing = batchId != null ? container.getRoastBatchRepository().findById(user.getId(), batchId) : null;

        FieldErrors parseErrors = new FieldErrors();
        LocalDate roastDate = parseDateField(req, "roastDate", parseErrors, "Fecha de tostión inválida (usa AAAA-MM-DD).");
        Double weightLoss = parseDoubleField(req, "weightLossPct", parseErrors, "Pérdida de masa inválida.");
        Double agtron = parseDoubleField(req, "agtron", parseErrors, "Valor Agtron inválido.");
        LocalDate openedOn = parseDateField(req, "openedOn", parseErrors, "Fecha de apertura inválida.");
        LocalDate preferredBy = parseDateField(req, "preferredByDate", parseErrors, "Fecha preferente inválida.");
        Double availWeight = parseDoubleField(req, "availableWeightG", parseErrors, "Peso disponible inválido.");

        RoastBatch candidate = new RoastBatch(
                existing != null ? existing.getId() : Ids.newId(),
                user.getId(),
                lot.getId(),
                WebSupport.trimOrNull(req.getParameter("batchCode")),
                roastDate,
                WebSupport.trimOrNull(req.getParameter("roastLevel")),
                WebSupport.trimOrNull(req.getParameter("roaster")),
                WebSupport.trimOrNull(req.getParameter("machine")),
                WebSupport.trimOrNull(req.getParameter("roastNotes")),
                weightLoss,
                agtron,
                openedOn,
                preferredBy,
                availWeight,
                existing != null && existing.isDemo(),
                existing != null ? existing.getArchivedAt() : null,
                existing != null ? existing.getCreatedAt() : now,
                now
        );

        FieldErrors domainErrors = CoffeeValidators.validateRoastBatch(candidate);
        for (Map.Entry<String, String> entry : domainErrors.asMap().entrySet()) {
            parseErrors.add(entry.getKey(), entry.getValue());
        }

        if (!parseErrors.isEmpty()) {
            req.setAttribute("batchErrors", parseErrors.asMap());
            req.setAttribute("editBatch", candidate);
            renderLotDetail(req, resp);
            return;
        }

        if (existing == null) {
            container.getRoastBatchRepository().create(candidate);
        } else {
            container.getRoastBatchRepository().update(candidate);
        }

        LocalDate today = LocalDate.now(container.getClock());
        RoastAge age = candidate.ageAt(today);
        if (age.isFuture()) {
            WebSupport.redirectWithFlash(req, resp, "/app/cafes/lote?id=" + lot.getId(), "warn",
                    "Lote de tostión guardado con ADVERTENCIA: la fecha de tostión (" + candidate.getRoastDate()
                            + ") es futura respecto a hoy (" + today + "). Verifica si es correcta.");
        } else {
            WebSupport.redirectWithFlash(req, resp, "/app/cafes/lote?id=" + lot.getId(), "success",
                    "Lote de tostión " + candidate.getBatchCode() + " guardado.");
        }
    }

    private void handleToggleArchiveBatch(HttpServletRequest req, HttpServletResponse resp, boolean archive)
            throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String batchId = WebSupport.trimOrNull(req.getParameter("batchId"));
        RoastBatch existing = container.getRoastBatchRepository().findById(user.getId(), batchId);
        if (existing == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/cafes", "error", "Lote de tostión no encontrado.");
            return;
        }
        Instant now = Instant.now(container.getClock());
        RoastBatch updated = new RoastBatch(
                existing.getId(), existing.getUserId(), existing.getCoffeeLotId(),
                existing.getBatchCode(), existing.getRoastDate(), existing.getRoastLevel(),
                existing.getRoaster(), existing.getMachine(), existing.getRoastNotes(),
                existing.getWeightLossPct(), existing.getAgtron(), existing.getOpenedOn(),
                existing.getPreferredByDate(), existing.getAvailableWeightG(),
                existing.isDemo(), archive ? now : null, existing.getCreatedAt(), now
        );
        container.getRoastBatchRepository().update(updated);
        WebSupport.redirectWithFlash(req, resp,
                "/app/cafes/lote?id=" + existing.getCoffeeLotId() + "&archived=true", "info",
                archive ? "Lote de tostión archivado." : "Lote de tostión restaurado.");
    }

    private static Integer parseIntField(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseIntOrNull(req.getParameter(name));
        } catch (NumberFormatException e) {
            errors.add(name, msg);
            return null;
        }
    }

    private static Double parseDoubleField(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseDoubleOrNull(req.getParameter(name));
        } catch (NumberFormatException e) {
            errors.add(name, msg);
            return null;
        }
    }

    private static LocalDate parseDateField(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseDateOrNull(req.getParameter(name));
        } catch (RuntimeException e) {
            errors.add(name, msg);
            return null;
        }
    }
}
