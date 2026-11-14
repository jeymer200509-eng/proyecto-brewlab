package com.brewlab.features.experiments;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.Ids;
import com.brewlab.domain.analytics.RuleRecommendationEngine;
import com.brewlab.domain.analytics.SessionComparison;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Experiment;
import com.brewlab.domain.model.ExperimentTrial;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.User;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador MVC de Experiment Lab (/app/experimentos).
 * Permite diseñar experimentos con pregunta, objetivo, hipótesis, una variable principal modificada,
 * variables controladas, ensayos (ExperimentTrial) vinculados a BrewSession, sugerencias basadas en reglas
 * y conclusión del usuario.
 */
public final class ExperimentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);

        List<Experiment> experiments = container.getExperimentRepository().findAllByUser(user.getId());
        List<BrewSession> sessions = container.getBrewSessionRepository().findAllByUser(user.getId());
        List<SensoryAssessment> assessments = container.getSensoryRepository().findAllByUser(user.getId());
        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), null, false);
        List<RoastBatch> batches = container.getRoastBatchRepository().findAllByUser(user.getId(), false);

        Map<String, BrewSession> sessionMap = new HashMap<>();
        for (BrewSession s : sessions) {
            sessionMap.put(s.getId(), s);
        }
        Map<String, SensoryAssessment> latestAssessmentBySession = new HashMap<>();
        for (SensoryAssessment a : assessments) {
            if (a.getBrewSessionId() != null && !latestAssessmentBySession.containsKey(a.getBrewSessionId())) {
                latestAssessmentBySession.put(a.getBrewSessionId(), a);
            }
        }
        Map<String, CoffeeLot> lotMap = new HashMap<>();
        for (CoffeeLot l : lots) {
            lotMap.put(l.getId(), l);
        }
        Map<String, RoastBatch> batchMap = new HashMap<>();
        for (RoastBatch b : batches) {
            batchMap.put(b.getId(), b);
        }

        Map<String, SessionComparison> comparisonByExperiment = new HashMap<>();
        Map<String, List<RuleRecommendationEngine.RuleSuggestion>> suggestionsByExperiment = new HashMap<>();

        for (Experiment exp : experiments) {
            List<BrewSession> expSessions = new ArrayList<>();
            for (ExperimentTrial t : exp.getTrials()) {
                BrewSession bs = sessionMap.get(t.getBrewSessionId());
                if (bs != null) {
                    expSessions.add(bs);
                }
            }
            comparisonByExperiment.put(exp.getId(),
                    SessionComparison.build(expSessions, latestAssessmentBySession));
            if (!expSessions.isEmpty()) {
                BrewSession lastSession = expSessions.get(expSessions.size() - 1);
                suggestionsByExperiment.put(exp.getId(),
                        RuleRecommendationEngine.evaluate(lastSession, latestAssessmentBySession.get(lastSession.getId())));
            } else {
                suggestionsByExperiment.put(exp.getId(), Collections.<RuleRecommendationEngine.RuleSuggestion>emptyList());
            }
        }

        req.setAttribute("experiments", experiments);
        req.setAttribute("brewSessions", sessions);
        req.setAttribute("coffeeLots", lots);
        req.setAttribute("roastBatches", batches);
        req.setAttribute("sessionMap", sessionMap);
        req.setAttribute("latestAssessmentBySession", latestAssessmentBySession);
        req.setAttribute("lotMap", lotMap);
        req.setAttribute("batchMap", batchMap);
        req.setAttribute("comparisonByExperiment", comparisonByExperiment);
        req.setAttribute("suggestionsByExperiment", suggestionsByExperiment);

        WebSupport.render(req, resp, "experimentos", "Experiment Lab — BrewLab", "experiments/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = WebSupport.trimOrNull(req.getParameter("action"));
        if ("createExperiment".equals(action)) {
            handleCreateExperiment(req, resp);
        } else if ("addTrial".equals(action)) {
            handleAddTrial(req, resp);
        } else if ("concludeExperiment".equals(action)) {
            handleConcludeExperiment(req, resp);
        } else if ("deleteExperiment".equals(action)) {
            handleDeleteExperiment(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/app/experimentos");
        }
    }

    private void handleCreateExperiment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String title = WebSupport.trimOrNull(req.getParameter("title"));
        String question = WebSupport.trimOrNull(req.getParameter("question"));
        String objective = WebSupport.trimOrNull(req.getParameter("objective"));
        String hypothesis = WebSupport.trimOrNull(req.getParameter("hypothesis"));
        String modifiedVariable = WebSupport.trimOrNull(req.getParameter("modifiedVariable"));
        String controlledVariables = WebSupport.trimOrNull(req.getParameter("controlledVariables"));
        String coffeeLotId = WebSupport.trimOrNull(req.getParameter("coffeeLotId"));
        String roastBatchId = WebSupport.trimOrNull(req.getParameter("roastBatchId"));

        if (title == null || question == null || objective == null || hypothesis == null
                || modifiedVariable == null || controlledVariables == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/experimentos", "error",
                    "Completa título, pregunta, objetivo, hipótesis, variable modificada y variables controladas.");
            return;
        }

        String expId = Ids.newId();
        List<ExperimentTrial> initialTrials = new ArrayList<>();
        String sessionA = WebSupport.trimOrNull(req.getParameter("trialSessionA"));
        String valueA = WebSupport.trimOrNull(req.getParameter("trialValueA"));
        String sessionB = WebSupport.trimOrNull(req.getParameter("trialSessionB"));
        String valueB = WebSupport.trimOrNull(req.getParameter("trialValueB"));

        if (sessionA != null) {
            initialTrials.add(new ExperimentTrial(
                    Ids.newId(), expId, sessionA, 1, "Ensayo A (Referencia)", valueA, null, now));
        }
        if (sessionB != null) {
            initialTrials.add(new ExperimentTrial(
                    Ids.newId(), expId, sessionB, initialTrials.size() + 1, "Ensayo B (Variación)", valueB, null, now));
        }

        Experiment exp = new Experiment(
                expId, user.getId(), coffeeLotId, roastBatchId,
                title, question, objective, hypothesis, modifiedVariable, controlledVariables,
                Experiment.STATUS_OPEN, null, false, initialTrials, now, now
        );
        container.getExperimentRepository().create(exp);
        WebSupport.redirectWithFlash(req, resp, "/app/experimentos", "success",
                "Experimento «" + title + "» registrado correctamente.");
    }

    private void handleAddTrial(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String experimentId = WebSupport.trimOrNull(req.getParameter("experimentId"));
        String brewSessionId = WebSupport.trimOrNull(req.getParameter("brewSessionId"));
        String label = WebSupport.trimOrNull(req.getParameter("label"));
        String modifiedValue = WebSupport.trimOrNull(req.getParameter("modifiedValue"));
        String notes = WebSupport.trimOrNull(req.getParameter("notes"));

        Experiment exp = container.getExperimentRepository().findById(user.getId(), experimentId);
        if (exp == null || brewSessionId == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/experimentos", "error",
                    "Selecciona un experimento y una sesión de preparación válida.");
            return;
        }
        int nextOrder = exp.getTrials().size() + 1;
        if (label == null) {
            label = "Ensayo #" + nextOrder;
        }
        ExperimentTrial trial = new ExperimentTrial(
                Ids.newId(), experimentId, brewSessionId, nextOrder, label, modifiedValue, notes, now
        );
        container.getExperimentRepository().addTrial(user.getId(), experimentId, trial, now);
        WebSupport.redirectWithFlash(req, resp, "/app/experimentos", "success",
                "Ensayo vinculado a la sesión de preparación.");
    }

    private void handleConcludeExperiment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String experimentId = WebSupport.trimOrNull(req.getParameter("experimentId"));
        String conclusion = WebSupport.trimOrNull(req.getParameter("userConclusion"));
        String status = WebSupport.trimOrNull(req.getParameter("status"));
        if (status == null) {
            status = Experiment.STATUS_CONCLUDED;
        }
        container.getExperimentRepository().updateConclusionAndStatus(
                user.getId(), experimentId, conclusion, status, Instant.now(container.getClock())
        );
        WebSupport.redirectWithFlash(req, resp, "/app/experimentos", "success",
                "Conclusión del experimento actualizada.");
    }

    private void handleDeleteExperiment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String experimentId = WebSupport.trimOrNull(req.getParameter("experimentId"));
        if (experimentId != null) {
            container.getExperimentRepository().delete(user.getId(), experimentId);
        }
        WebSupport.redirectWithFlash(req, resp, "/app/experimentos", "info", "Experimento eliminado.");
    }
}
