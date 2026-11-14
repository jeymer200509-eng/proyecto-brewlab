package com.brewlab.features.analytics;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.analytics.RuleRecommendationEngine;
import com.brewlab.domain.analytics.SessionComparison;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.User;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controlador MVC de Analítica y Comparador de Sesiones (/app/analisis).
 * Permite filtrar el historial por café, tueste, método, receta, equipo y rango de fechas,
 * y comparar 2 o más sesiones con advertencias metodológicas, gráficos SVG y tabla fuente.
 */
public final class AnalyticsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        ZoneId zone = container.getConfig().getTimezone();

        String filterLotId = WebSupport.trimOrNull(req.getParameter("coffeeLotId"));
        String filterBatchId = WebSupport.trimOrNull(req.getParameter("roastBatchId"));
        String filterMethod = WebSupport.trimOrNull(req.getParameter("method"));
        String filterRecipeId = WebSupport.trimOrNull(req.getParameter("recipeId"));
        String filterEquipmentId = WebSupport.trimOrNull(req.getParameter("equipmentId"));
        LocalDate filterFrom = WebSupport.parseDateOrNull(req.getParameter("dateFrom"));
        LocalDate filterTo = WebSupport.parseDateOrNull(req.getParameter("dateTo"));

        List<BrewSession> allSessions = container.getBrewSessionRepository().findAllByUser(user.getId());
        List<SensoryAssessment> allAssessments = container.getSensoryRepository().findAllByUser(user.getId());
        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), null, true);
        List<RoastBatch> batches = container.getRoastBatchRepository().findAllByUser(user.getId(), true);
        List<Recipe> recipes = container.getRecipeRepository().findAllByUser(user.getId());
        List<Equipment> equipmentList = container.getEquipmentRepository().findAllByUser(user.getId());

        Map<String, SensoryAssessment> latestAssessmentBySession = new HashMap<>();
        for (SensoryAssessment a : allAssessments) {
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

        Set<String> availableMethods = new LinkedHashSet<>();
        List<BrewSession> filteredSessions = new ArrayList<>();
        for (BrewSession s : allSessions) {
            if (s.getPlannedMethod() != null) {
                availableMethods.add(s.getPlannedMethod());
            }
            if (filterLotId != null && !filterLotId.equals(s.getCoffeeLotId())) {
                continue;
            }
            if (filterBatchId != null && !filterBatchId.equals(s.getRoastBatchId())) {
                continue;
            }
            if (filterMethod != null && !filterMethod.equalsIgnoreCase(s.getPlannedMethod())) {
                continue;
            }
            if (filterRecipeId != null && !filterRecipeId.equals(s.getRecipeId())) {
                continue;
            }
            if (filterEquipmentId != null && !s.getEquipmentIds().contains(filterEquipmentId)) {
                continue;
            }
            if (s.getCreatedAt() != null) {
                LocalDate sessionDate = s.getCreatedAt().atZone(zone).toLocalDate();
                if (filterFrom != null && sessionDate.isBefore(filterFrom)) {
                    continue;
                }
                if (filterTo != null && sessionDate.isAfter(filterTo)) {
                    continue;
                }
            }
            filteredSessions.add(s);
        }

        String[] compareIdArr = req.getParameterValues("compareId");
        Set<String> selectedCompareIds = new LinkedHashSet<>();
        if (compareIdArr != null && compareIdArr.length > 0) {
            selectedCompareIds.addAll(Arrays.asList(compareIdArr));
        } else if (filteredSessions.size() >= 2) {
            selectedCompareIds.add(filteredSessions.get(0).getId());
            selectedCompareIds.add(filteredSessions.get(1).getId());
        }

        List<BrewSession> comparedSessions = new ArrayList<>();
        for (BrewSession s : allSessions) {
            if (selectedCompareIds.contains(s.getId())) {
                comparedSessions.add(s);
            }
        }

        SessionComparison comparison = SessionComparison.build(comparedSessions, latestAssessmentBySession);

        Map<String, List<RuleRecommendationEngine.RuleSuggestion>> suggestionsBySession = new HashMap<>();
        for (BrewSession cs : comparedSessions) {
            suggestionsBySession.put(cs.getId(),
                    RuleRecommendationEngine.evaluate(cs, latestAssessmentBySession.get(cs.getId())));
        }

        req.setAttribute("filteredSessions", filteredSessions);
        req.setAttribute("totalSessionCount", Integer.valueOf(allSessions.size()));
        req.setAttribute("selectedCompareIds", selectedCompareIds);
        req.setAttribute("comparison", comparison);
        req.setAttribute("latestAssessmentBySession", latestAssessmentBySession);
        req.setAttribute("suggestionsBySession", suggestionsBySession);
        req.setAttribute("coffeeLots", lots);
        req.setAttribute("roastBatches", batches);
        req.setAttribute("recipes", recipes);
        req.setAttribute("equipmentList", equipmentList);
        req.setAttribute("availableMethods", availableMethods);
        req.setAttribute("lotMap", lotMap);
        req.setAttribute("batchMap", batchMap);

        req.setAttribute("filterLotId", filterLotId);
        req.setAttribute("filterBatchId", filterBatchId);
        req.setAttribute("filterMethod", filterMethod);
        req.setAttribute("filterRecipeId", filterRecipeId);
        req.setAttribute("filterEquipmentId", filterEquipmentId);
        req.setAttribute("filterDateFrom", req.getParameter("dateFrom"));
        req.setAttribute("filterDateTo", req.getParameter("dateTo"));

        WebSupport.render(req, resp, "analisis", "Analítica y Comparador de Sesiones — BrewLab", "analytics/index.jsp");
    }
}
