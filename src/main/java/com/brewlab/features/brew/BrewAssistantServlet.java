package com.brewlab.features.brew;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.Ids;
import com.brewlab.domain.calc.BrewTimerEngine;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.UserSettings;
import com.brewlab.domain.model.WaterProfile;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Controlador MVC del Asistente de Preparación (Brew Assistant, /app/preparacion). */
public final class BrewAssistantServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        boolean forceNew = "true".equalsIgnoreCase(req.getParameter("new"));

        BrewSession currentSession = null;
        if (sessionId != null) {
            currentSession = container.getBrewSessionRepository().findById(user.getId(), sessionId);
        } else if (!forceNew) {
            currentSession = container.getBrewSessionRepository().findLatestInProgress(user.getId());
        }

        if (currentSession != null) {
            long elapsedMs = BrewTimerEngine.computeElapsedMs(currentSession, now);
            int elapsedSeconds = (int) Math.round(elapsedMs / 1000.0);
            RecipeStep currentStep = BrewTimerEngine.findCurrentStep(currentSession);
            RecipeStep nextStep = BrewTimerEngine.findNextStep(currentSession);
            Set<Integer> handledOrders = BrewTimerEngine.handledStepOrders(currentSession.getEvents());

            req.setAttribute("brewSession", currentSession);
            req.setAttribute("elapsedMs", Long.valueOf(elapsedMs));
            req.setAttribute("elapsedFormatted", Formats.duration(Integer.valueOf(elapsedSeconds)));
            req.setAttribute("serverNowEpochMs", Long.valueOf(now.toEpochMilli()));
            req.setAttribute("currentStep", currentStep);
            req.setAttribute("nextStep", nextStep);
            req.setAttribute("handledStepOrders", handledOrders);
            req.setAttribute("actionNonce", Ids.newId());

            if (currentSession.getCoffeeLotId() != null) {
                req.setAttribute("sessionLot", container.getCoffeeLotRepository()
                        .findById(user.getId(), currentSession.getCoffeeLotId()));
            }
            if (currentSession.getRoastBatchId() != null) {
                RoastBatch rb = container.getRoastBatchRepository()
                        .findById(user.getId(), currentSession.getRoastBatchId());
                req.setAttribute("sessionBatch", rb);
                if (rb != null) {
                    req.setAttribute("sessionBatchAge",
                            rb.ageAt(java.time.LocalDate.now(container.getClock())));
                }
            }
        }

        List<Recipe> recipes = container.getRecipeRepository().findAllByUser(user.getId());
        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), null, false);
        List<RoastBatch> batches = container.getRoastBatchRepository().findAllByUser(user.getId(), false);
        List<Equipment> equipmentList = container.getEquipmentRepository().findAllByUser(user.getId());
        List<WaterProfile> waterProfiles = container.getWaterProfileRepository().findAllByUser(user.getId());
        List<BrewSession> history = container.getBrewSessionRepository().findAllByUser(user.getId());
        UserSettings settings = container.getUserSettingsRepository().findOrDefault(user.getId());

        Map<String, CoffeeLot> lotMap = new HashMap<>();
        for (CoffeeLot l : lots) {
            lotMap.put(l.getId(), l);
        }
        Map<String, RoastBatch> batchMap = new HashMap<>();
        for (RoastBatch b : batches) {
            batchMap.put(b.getId(), b);
        }

        req.setAttribute("recipes", recipes);
        req.setAttribute("coffeeLots", lots);
        req.setAttribute("roastBatches", batches);
        req.setAttribute("lotMap", lotMap);
        req.setAttribute("batchMap", batchMap);
        req.setAttribute("equipmentList", equipmentList);
        req.setAttribute("waterProfiles", waterProfiles);
        req.setAttribute("sessionHistory", history);
        req.setAttribute("settings", settings);
        req.setAttribute("preselectedRecipeId", WebSupport.trimOrNull(req.getParameter("recipeId")));
        req.setAttribute("preselectedLotId", WebSupport.trimOrNull(req.getParameter("coffeeLotId")));
        req.setAttribute("preselectedBatchId", WebSupport.trimOrNull(req.getParameter("roastBatchId")));

        WebSupport.render(req, resp, "preparacion", "Asistente de Preparación — BrewLab", "brew/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = WebSupport.trimOrNull(req.getParameter("action"));
        if ("createSession".equals(action)) {
            handleCreateSession(req, resp);
        } else if ("start".equals(action)) {
            handleStart(req, resp);
        } else if ("pause".equals(action)) {
            handlePause(req, resp);
        } else if ("resume".equals(action)) {
            handleResume(req, resp);
        } else if ("completeStep".equals(action) || "skipStep".equals(action)) {
            handleStepAction(req, resp, "skipStep".equals(action));
        } else if ("completeSession".equals(action)) {
            handleCompleteSession(req, resp);
        } else if ("cancelSession".equals(action)) {
            handleCancelSession(req, resp);
        } else if ("correctSession".equals(action)) {
            handleCorrectSession(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/app/preparacion");
        }
    }

    private void handleCreateSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String recipeId = WebSupport.trimOrNull(req.getParameter("recipeId"));
        Recipe recipe = container.getRecipeRepository().findById(user.getId(), recipeId);
        if (recipe == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/preparacion?new=true", "error",
                    "Selecciona una receta válida para iniciar la preparación.");
            return;
        }

        String coffeeLotId = WebSupport.trimOrNull(req.getParameter("coffeeLotId"));
        if (coffeeLotId == null) {
            coffeeLotId = recipe.getCoffeeLotId();
        }
        String roastBatchId = WebSupport.trimOrNull(req.getParameter("roastBatchId"));
        if (roastBatchId == null) {
            roastBatchId = recipe.getRoastBatchId();
        }
        if (roastBatchId != null && coffeeLotId == null) {
            RoastBatch rb = container.getRoastBatchRepository().findById(user.getId(), roastBatchId);
            if (rb != null) {
                coffeeLotId = rb.getCoffeeLotId();
            }
        }

        String waterProfileId = WebSupport.trimOrNull(req.getParameter("waterProfileId"));
        if (waterProfileId == null) {
            waterProfileId = recipe.getWaterProfileId();
        }

        String[] eqArr = req.getParameterValues("equipmentIds");
        List<String> eqIds = eqArr != null ? Arrays.asList(eqArr) : recipe.getEquipmentIds();

        Double actualDoseG = parseOptionalDouble(req.getParameter("actualDoseG"), recipe.getDoseG());
        Double actualTempC = parseOptionalDouble(req.getParameter("actualTemperatureC"), recipe.getTemperatureC());
        String actualGrind = WebSupport.trimOrNull(req.getParameter("actualGrindSetting"));
        if (actualGrind == null) {
            actualGrind = recipe.getGrindSetting();
        }

        Instant now = Instant.now(container.getClock());
        String sessionId = Ids.newId();
        String snapshotJson = BrewSession.serializeStepsSnapshot(recipe.getSteps());

        boolean startImmediately = "true".equalsIgnoreCase(req.getParameter("startImmediately"));
        String initialStatus = startImmediately ? BrewSession.STATUS_ACTIVE : BrewSession.STATUS_PREPARED;
        Instant startedAt = startImmediately ? now : null;
        List<BrewEvent> initialEvents = startImmediately
                ? Collections.singletonList(new BrewEvent(
                        Ids.newId(), sessionId, null, null, BrewEvent.TYPE_STARTED,
                        now, 0L, recipe.getWaterG(), null, "START", "Inicio de cronómetro"))
                : Collections.<BrewEvent>emptyList();

        BrewSession session = new BrewSession(
                sessionId, user.getId(), recipe.getId(), coffeeLotId, roastBatchId, waterProfileId, eqIds,
                initialStatus, startedAt, null, 0L, null,
                recipe.getName(), recipe.getMethod(), recipe.getDoseG(), recipe.getWaterG(),
                recipe.getTemperatureC(), recipe.getGrindSetting(),
                recipe.getTargetTimeMinS(), recipe.getTargetTimeMaxS(),
                snapshotJson,
                actualDoseG, null, actualTempC, actualGrind, null, null,
                WebSupport.trimOrNull(req.getParameter("notes")),
                false, initialEvents, now, now
        );

        container.getBrewSessionRepository().create(session);
        WebSupport.redirectWithFlash(req, resp, "/app/preparacion?sessionId=" + sessionId,
                "success", startImmediately
                        ? "Sesión iniciada. El cronómetro está corriendo con instantánea de la receta."
                        : "Sesión preparada con instantánea de la receta. Pulsa «Iniciar cronómetro» al verter.");
    }

    private void handleStart(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));
        container.getBrewSessionRepository().startSession(
                user.getId(), sessionId, Instant.now(container.getClock()), idem);
        resp.sendRedirect(req.getContextPath() + "/app/preparacion?sessionId=" + sessionId);
    }

    private void handlePause(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));
        container.getBrewSessionRepository().pauseSession(
                user.getId(), sessionId, Instant.now(container.getClock()), idem);
        resp.sendRedirect(req.getContextPath() + "/app/preparacion?sessionId=" + sessionId);
    }

    private void handleResume(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));
        container.getBrewSessionRepository().resumeSession(
                user.getId(), sessionId, Instant.now(container.getClock()), idem);
        resp.sendRedirect(req.getContextPath() + "/app/preparacion?sessionId=" + sessionId);
    }

    private void handleStepAction(HttpServletRequest req, HttpServletResponse resp, boolean skipped)
            throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        BrewSession session = container.getBrewSessionRepository().findById(user.getId(), sessionId);
        if (session == null) {
            resp.sendRedirect(req.getContextPath() + "/app/preparacion");
            return;
        }
        if (skipped && !"true".equalsIgnoreCase(req.getParameter("confirmSkip"))) {
            WebSupport.redirectWithFlash(req, resp, "/app/preparacion?sessionId=" + sessionId,
                    "warn", "Marca la casilla de confirmación antes de saltar una etapa.");
            return;
        }

        RecipeStep currentStep = BrewTimerEngine.findCurrentStep(session);
        if (currentStep == null) {
            resp.sendRedirect(req.getContextPath() + "/app/preparacion?sessionId=" + sessionId);
            return;
        }

        Double actualWater = parseOptionalDouble(req.getParameter("stepActualWaterG"), null);
        String stepNotes = WebSupport.trimOrNull(req.getParameter("stepNotes"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));

        container.getBrewSessionRepository().recordStepEvent(
                user.getId(), sessionId, currentStep, skipped, actualWater, stepNotes,
                Instant.now(container.getClock()), idem
        );
        resp.sendRedirect(req.getContextPath() + "/app/preparacion?sessionId=" + sessionId);
    }

    private void handleCompleteSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        Double actualDose = parseOptionalDouble(req.getParameter("actualDoseG"), null);
        Double actualWater = parseOptionalDouble(req.getParameter("actualWaterG"), null);
        Double actualTemp = parseOptionalDouble(req.getParameter("actualTemperatureC"), null);
        String actualGrind = WebSupport.trimOrNull(req.getParameter("actualGrindSetting"));
        Integer overrideTime = parseOptionalInt(req.getParameter("totalTimeSeconds"));
        Double finalBeverage = parseOptionalDouble(req.getParameter("finalBeverageWeightG"), null);
        String notes = WebSupport.trimOrNull(req.getParameter("notes"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));

        container.getBrewSessionRepository().completeSession(
                user.getId(), sessionId, actualDose, actualWater, actualTemp, actualGrind,
                overrideTime, finalBeverage, notes, Instant.now(container.getClock()), idem
        );
        WebSupport.redirectWithFlash(req, resp, "/app/preparacion?sessionId=" + sessionId,
                "success", "Sesión finalizada y guardada como registro histórico. Puedes evaluarla sensorialmente.");
    }

    private void handleCancelSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        String reason = WebSupport.trimOrNull(req.getParameter("cancelReason"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));

        container.getBrewSessionRepository().cancelSession(
                user.getId(), sessionId, reason, Instant.now(container.getClock()), idem);
        WebSupport.redirectWithFlash(req, resp, "/app/preparacion?sessionId=" + sessionId,
                "info", "Sesión marcada como cancelada.");
    }

    private void handleCorrectSession(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String sessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        Double actualDose = parseOptionalDouble(req.getParameter("actualDoseG"), null);
        Double actualWater = parseOptionalDouble(req.getParameter("actualWaterG"), null);
        Double actualTemp = parseOptionalDouble(req.getParameter("actualTemperatureC"), null);
        String actualGrind = WebSupport.trimOrNull(req.getParameter("actualGrindSetting"));
        Integer totalTime = parseOptionalInt(req.getParameter("totalTimeSeconds"));
        Double finalBeverage = parseOptionalDouble(req.getParameter("finalBeverageWeightG"), null);
        String reason = WebSupport.trimOrNull(req.getParameter("correctionReason"));
        String idem = WebSupport.trimOrNull(req.getParameter("idempotencyKey"));

        container.getBrewSessionRepository().recordAuditableCorrection(
                user.getId(), sessionId, actualDose, actualWater, actualTemp, actualGrind,
                totalTime, finalBeverage, reason, Instant.now(container.getClock()), idem
        );
        WebSupport.redirectWithFlash(req, resp, "/app/preparacion?sessionId=" + sessionId,
                "info", "Corrección registrada como evento auditable en el historial de la sesión.");
    }

    private static Double parseOptionalDouble(String raw, Double fallback) {
        try {
            Double v = WebSupport.parseDoubleOrNull(raw);
            return v != null ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Integer parseOptionalInt(String raw) {
        try {
            return WebSupport.parseIntOrNull(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
