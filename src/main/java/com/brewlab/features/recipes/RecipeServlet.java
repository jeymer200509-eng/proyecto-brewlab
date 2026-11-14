package com.brewlab.features.recipes;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.Ids;
import com.brewlab.domain.calc.BrewRatio;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;
import com.brewlab.domain.model.RoastBatch;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Controlador MVC para el Constructor de Recetas y Pasos (/app/recetas y /app/recetas/detalle). */
public final class RecipeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("/app/recetas/detalle".equals(req.getServletPath())) {
            renderDetail(req, resp);
        } else {
            renderList(req, resp);
        }
    }

    private void renderList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);

        List<Recipe> recipes = container.getRecipeRepository().findAllByUser(user.getId());
        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), null, false);
        List<RoastBatch> batches = container.getRoastBatchRepository().findAllByUser(user.getId(), false);
        List<Equipment> equipmentList = container.getEquipmentRepository().findAllByUser(user.getId());
        List<WaterProfile> waterProfiles = container.getWaterProfileRepository().findAllByUser(user.getId());

        Map<String, List<String>> warningsByRecipe = new HashMap<>();
        for (Recipe r : recipes) {
            warningsByRecipe.put(r.getId(), RecipeValidators.inspectCoherenceWarnings(r));
        }

        req.setAttribute("recipes", recipes);
        req.setAttribute("warningsByRecipe", warningsByRecipe);
        req.setAttribute("coffeeLots", lots);
        req.setAttribute("roastBatches", batches);
        req.setAttribute("equipmentList", equipmentList);
        req.setAttribute("waterProfiles", waterProfiles);
        req.setAttribute("ratioFormula", BrewRatio.FORMULA);
        WebSupport.render(req, resp, "recetas", "Recetas y Métodos — BrewLab", "recipes/list.jsp");
    }

    private void renderDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String id = WebSupport.trimOrNull(req.getParameter("id"));
        Recipe recipe = container.getRecipeRepository().findById(user.getId(), id);
        if (recipe == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/recetas", "error", "Receta no encontrada.");
            return;
        }

        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), null, true);
        List<RoastBatch> batches = container.getRoastBatchRepository().findAllByUser(user.getId(), true);
        List<Equipment> equipmentList = container.getEquipmentRepository().findAllByUser(user.getId());
        List<WaterProfile> waterProfiles = container.getWaterProfileRepository().findAllByUser(user.getId());

        String editStepId = WebSupport.trimOrNull(req.getParameter("editStepId"));
        if (editStepId != null && req.getAttribute("editStep") == null) {
            for (RecipeStep s : recipe.getSteps()) {
                if (s.getId().equals(editStepId)) {
                    req.setAttribute("editStep", s);
                    break;
                }
            }
        }

        req.setAttribute("recipe", recipe);
        req.setAttribute("coherenceWarnings", RecipeValidators.inspectCoherenceWarnings(recipe));
        req.setAttribute("coffeeLots", lots);
        req.setAttribute("roastBatches", batches);
        req.setAttribute("equipmentList", equipmentList);
        req.setAttribute("waterProfiles", waterProfiles);
        req.setAttribute("ratioFormula", BrewRatio.FORMULA);
        WebSupport.render(req, resp, "recetas", recipe.getName() + " — Editor de Receta", "recipes/detail.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = WebSupport.trimOrNull(req.getParameter("action"));
        if ("saveRecipe".equals(action)) {
            handleSaveRecipe(req, resp);
        } else if ("duplicateRecipe".equals(action)) {
            handleDuplicateRecipe(req, resp);
        } else if ("deleteRecipe".equals(action)) {
            handleDeleteRecipe(req, resp);
        } else if ("createV60Example".equals(action)) {
            handleCreateV60Example(req, resp);
        } else if ("saveStep".equals(action)) {
            handleSaveStep(req, resp);
        } else if ("deleteStep".equals(action)) {
            handleDeleteStep(req, resp);
        } else if ("duplicateStep".equals(action)) {
            handleDuplicateStep(req, resp);
        } else if ("moveStepUp".equals(action) || "moveStepDown".equals(action)) {
            handleMoveStep(req, resp, "moveStepUp".equals(action) ? -1 : 1);
        } else {
            resp.sendRedirect(req.getContextPath() + "/app/recetas");
        }
    }

    private void handleSaveRecipe(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String id = WebSupport.trimOrNull(req.getParameter("id"));
        Recipe existing = id != null ? container.getRecipeRepository().findById(user.getId(), id) : null;

        FieldErrors errors = new FieldErrors();
        Double doseG = parseDouble(req, "doseG", errors, "Dosis de café inválida.");
        Double waterG = parseDouble(req, "waterG", errors, "Agua total inválida.");
        Double tempC = parseDouble(req, "temperatureC", errors, "Temperatura inválida.");
        Integer timeMin = parseInt(req, "targetTimeMinS", errors, "Tiempo mínimo inválido.");
        Integer timeMax = parseInt(req, "targetTimeMaxS", errors, "Tiempo máximo inválido.");

        String[] eqArr = req.getParameterValues("equipmentIds");
        List<String> eqIds = eqArr != null ? Arrays.asList(eqArr) : Collections.<String>emptyList();

        Recipe candidate = new Recipe(
                existing != null ? existing.getId() : Ids.newId(),
                user.getId(),
                WebSupport.trimOrNull(req.getParameter("name")),
                WebSupport.trimOrNull(req.getParameter("method")),
                WebSupport.trimOrNull(req.getParameter("coffeeLotId")),
                WebSupport.trimOrNull(req.getParameter("roastBatchId")),
                doseG,
                waterG,
                tempC,
                WebSupport.trimOrNull(req.getParameter("grindSetting")),
                timeMin,
                timeMax,
                eqIds,
                WebSupport.trimOrNull(req.getParameter("waterProfileId")),
                WebSupport.trimOrNull(req.getParameter("notes")),
                existing != null ? existing.getVersion() : 1,
                existing != null && existing.isDemo(),
                existing != null ? existing.getSteps() : Collections.<RecipeStep>emptyList(),
                existing != null ? existing.getCreatedAt() : now,
                now
        );

        FieldErrors domainErrors = RecipeValidators.validateRecipe(candidate);
        for (Map.Entry<String, String> e : domainErrors.asMap().entrySet()) {
            errors.add(e.getKey(), e.getValue());
        }

        if (!errors.isEmpty()) {
            req.setAttribute("recipeErrors", errors.asMap());
            req.setAttribute("editRecipe", candidate);
            if (existing != null) {
                renderDetail(req, resp);
            } else {
                renderList(req, resp);
            }
            return;
        }

        if (existing == null) {
            container.getRecipeRepository().create(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + candidate.getId(),
                    "success", "Receta creada (ratio calculado " + candidate.getRatioDisplay() + "). Agrega los pasos de preparación.");
        } else {
            container.getRecipeRepository().updateHeaderAndEquipment(candidate);
            WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + candidate.getId(),
                    "success", "Parámetros previstos de la receta actualizados (ratio " + candidate.getRatioDisplay() + ").");
        }
    }

    private void handleDuplicateRecipe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String id = WebSupport.trimOrNull(req.getParameter("id"));
        Recipe copy = container.getRecipeRepository().duplicate(
                user.getId(), id, Instant.now(container.getClock()));
        if (copy == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/recetas", "error", "No se encontró la receta a duplicar.");
            return;
        }
        WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + copy.getId(),
                "success", "Receta duplicada con nuevo ID (" + copy.getName() + ").");
    }

    private void handleDeleteRecipe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String id = WebSupport.trimOrNull(req.getParameter("id"));
        if (id != null) {
            container.getRecipeRepository().delete(user.getId(), id);
        }
        WebSupport.redirectWithFlash(req, resp, "/app/recetas", "info", "Receta eliminada.");
    }

    private void handleCreateV60Example(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());
        String rId = Ids.newId();
        List<RecipeStep> steps = new ArrayList<>();
        steps.add(new RecipeStep(Ids.newId(), rId, 1, "Preinfusión",
                "Verter en espiral desde el centro para humedecer toda la cama de café.",
                0, Integer.valueOf(45), Double.valueOf(45.0), Double.valueOf(45.0), Integer.valueOf(5)));
        steps.add(new RecipeStep(Ids.newId(), rId, 2, "Primer vertido principal",
                "Verter con flujo constante hasta alcanzar 150 g acumulados.",
                45, Integer.valueOf(45), Double.valueOf(105.0), Double.valueOf(150.0), Integer.valueOf(5)));
        steps.add(new RecipeStep(Ids.newId(), rId, 3, "Segundo vertido",
                "Verter suavemente en el centro hasta completar 250 g acumulados.",
                90, Integer.valueOf(45), Double.valueOf(100.0), Double.valueOf(250.0), Integer.valueOf(5)));
        steps.add(new RecipeStep(Ids.newId(), rId, 4, "Drenaje final",
                "Esperar el drenaje completo de la cama y registrar el tiempo final.",
                135, Integer.valueOf(45), Double.valueOf(0.0), Double.valueOf(250.0), Integer.valueOf(15)));

        Recipe example = new Recipe(
                rId, user.getId(), "V60 Base 15 g / 250 g (Ejemplo editable)", "V60",
                null, null, Double.valueOf(15.0), Double.valueOf(250.0), Double.valueOf(93.0),
                "Media-fina (referencia editable)", Integer.valueOf(165), Integer.valueOf(195),
                Collections.<String>emptyList(), null,
                "Punto de partida ilustrativo (Sección 06 de la especificación). Ajusta según el café y tu molino.",
                1, true, steps, now, now
        );
        container.getRecipeRepository().create(example);
        WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + rId,
                "info", "Plantilla ilustrativa V60 (15 g / 250 g → 1:16,7) creada como punto de partida editable.");
    }

    private void handleSaveStep(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String recipeId = WebSupport.trimOrNull(req.getParameter("recipeId"));
        Recipe recipe = container.getRecipeRepository().findById(user.getId(), recipeId);
        if (recipe == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/recetas", "error", "Receta no encontrada.");
            return;
        }

        FieldErrors errors = new FieldErrors();
        Integer startS = parseInt(req, "startSeconds", errors, "Inicio (s) inválido.");
        Integer durS = parseInt(req, "durationSeconds", errors, "Duración (s) inválida.");
        Double waterDose = parseDouble(req, "waterDoseG", errors, "Agua del paso inválida.");
        Double cumWater = parseDouble(req, "cumulativeWaterG", errors, "Agua acumulada inválida.");
        Integer tolS = parseInt(req, "toleranceSeconds", errors, "Tolerancia inválida.");

        String stepId = WebSupport.trimOrNull(req.getParameter("stepId"));
        RecipeStep step = new RecipeStep(
                stepId != null ? stepId : Ids.newId(),
                recipeId,
                recipe.getSteps().size() + 1,
                WebSupport.trimOrNull(req.getParameter("title")),
                WebSupport.trimOrNull(req.getParameter("instruction")),
                startS != null ? startS.intValue() : -1,
                durS,
                waterDose,
                cumWater,
                tolS
        );

        FieldErrors domainErrors = RecipeValidators.validateStep(step);
        for (Map.Entry<String, String> e : domainErrors.asMap().entrySet()) {
            errors.add(e.getKey(), e.getValue());
        }

        if (!errors.isEmpty()) {
            req.setAttribute("stepErrors", errors.asMap());
            req.setAttribute("editStep", step);
            renderDetail(req, resp);
            return;
        }

        container.getRecipeRepository().saveStep(user.getId(), recipeId, step, Instant.now(container.getClock()));
        WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + recipeId + "#pasos-receta",
                "success", "Paso guardado.");
    }

    private void handleDeleteStep(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String recipeId = WebSupport.trimOrNull(req.getParameter("recipeId"));
        String stepId = WebSupport.trimOrNull(req.getParameter("stepId"));
        container.getRecipeRepository().deleteStep(user.getId(), recipeId, stepId, Instant.now(container.getClock()));
        WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + recipeId + "#pasos-receta",
                "info", "Paso eliminado y numeración actualizada.");
    }

    private void handleDuplicateStep(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String recipeId = WebSupport.trimOrNull(req.getParameter("recipeId"));
        String stepId = WebSupport.trimOrNull(req.getParameter("stepId"));
        container.getRecipeRepository().duplicateStep(user.getId(), recipeId, stepId, Instant.now(container.getClock()));
        WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + recipeId + "#pasos-receta",
                "success", "Paso duplicado.");
    }

    private void handleMoveStep(HttpServletRequest req, HttpServletResponse resp, int delta) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String recipeId = WebSupport.trimOrNull(req.getParameter("recipeId"));
        String stepId = WebSupport.trimOrNull(req.getParameter("stepId"));
        container.getRecipeRepository().moveStep(user.getId(), recipeId, stepId, delta, Instant.now(container.getClock()));
        WebSupport.redirectWithFlash(req, resp, "/app/recetas/detalle?id=" + recipeId + "#pasos-receta",
                "info", "Orden de pasos actualizado.");
    }

    private static Double parseDouble(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseDoubleOrNull(req.getParameter(name));
        } catch (NumberFormatException e) {
            errors.add(name, msg);
            return null;
        }
    }

    private static Integer parseInt(HttpServletRequest req, String name, FieldErrors errors, String msg) {
        try {
            return WebSupport.parseIntOrNull(req.getParameter(name));
        } catch (NumberFormatException e) {
            errors.add(name, msg);
            return null;
        }
    }
}
