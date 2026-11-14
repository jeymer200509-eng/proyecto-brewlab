package com.brewlab.features.sensory;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.AssessmentDescriptor;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.PreferenceRecord;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.SensoryDescriptor;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.UserSettings;
import com.brewlab.domain.sensory.OriginalFlavorTaxonomy;
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

/**
 * Controlador MVC de Sensory Lab y Flavor Explorer (/app/catacion).
 * Gestiona evaluaciones descriptivas, explorador jerárquico de descriptores (Familia → Subfamilia → Descriptor),
 * descriptores personalizados y bloque afectivo separado (PreferenceRecord).
 */
public final class SensoryLabServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        UserSettings settings = container.getUserSettingsRepository().findOrDefault(user.getId());

        List<SensoryDescriptor> descriptors = container.getSensoryRepository().findAllDescriptors(user.getId());
        List<OriginalFlavorTaxonomy.WheelSector> familySectors = OriginalFlavorTaxonomy.buildFamilySectors(descriptors);
        List<OriginalFlavorTaxonomy.WheelSector> subfamilySectors = OriginalFlavorTaxonomy.buildSubfamilySectors(descriptors);

        List<CoffeeLot> lots = container.getCoffeeLotRepository().findAll(user.getId(), null, false);
        List<RoastBatch> batches = container.getRoastBatchRepository().findAllByUser(user.getId(), false);
        List<BrewSession> sessions = container.getBrewSessionRepository().findAllByUser(user.getId());
        List<SensoryAssessment> assessments = container.getSensoryRepository().findAllByUser(user.getId());

        Map<String, CoffeeLot> lotMap = new HashMap<>();
        for (CoffeeLot l : lots) {
            lotMap.put(l.getId(), l);
        }
        Map<String, RoastBatch> batchMap = new HashMap<>();
        for (RoastBatch b : batches) {
            batchMap.put(b.getId(), b);
        }
        Map<String, BrewSession> sessionMap = new HashMap<>();
        for (BrewSession s : sessions) {
            sessionMap.put(s.getId(), s);
        }

        String preselectedSessionId = WebSupport.trimOrNull(req.getParameter("sessionId"));
        String preselectedLotId = WebSupport.trimOrNull(req.getParameter("coffeeLotId"));
        String preselectedBatchId = WebSupport.trimOrNull(req.getParameter("roastBatchId"));

        if (preselectedSessionId != null) {
            BrewSession linked = sessionMap.get(preselectedSessionId);
            if (linked != null) {
                if (preselectedLotId == null) {
                    preselectedLotId = linked.getCoffeeLotId();
                }
                if (preselectedBatchId == null) {
                    preselectedBatchId = linked.getRoastBatchId();
                }
            }
        }

        req.setAttribute("descriptors", descriptors);
        req.setAttribute("familySectors", familySectors);
        req.setAttribute("subfamilySectors", subfamilySectors);
        req.setAttribute("taxonomyVersion", OriginalFlavorTaxonomy.VERSION);
        req.setAttribute("taxonomyAttribution", OriginalFlavorTaxonomy.ATTRIBUTION);
        req.setAttribute("coffeeLots", lots);
        req.setAttribute("roastBatches", batches);
        req.setAttribute("brewSessions", sessions);
        req.setAttribute("assessments", assessments);
        req.setAttribute("lotMap", lotMap);
        req.setAttribute("batchMap", batchMap);
        req.setAttribute("sessionMap", sessionMap);
        req.setAttribute("settings", settings);
        req.setAttribute("todayIso", LocalDate.now(container.getClock()).toString());
        req.setAttribute("preselectedSessionId", preselectedSessionId);
        req.setAttribute("preselectedLotId", preselectedLotId);
        req.setAttribute("preselectedBatchId", preselectedBatchId);

        WebSupport.render(req, resp, "catacion", "Sensory Lab y Flavor Explorer — BrewLab", "sensory/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = WebSupport.trimOrNull(req.getParameter("action"));
        if ("createAssessment".equals(action)) {
            handleCreateAssessment(req, resp);
        } else if ("createCustomDescriptor".equals(action)) {
            handleCreateCustomDescriptor(req, resp);
        } else if ("deleteAssessment".equals(action)) {
            handleDeleteAssessment(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/app/catacion");
        }
    }

    private void handleCreateAssessment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        Instant now = Instant.now(container.getClock());

        String evaluatorName = WebSupport.trimOrNull(req.getParameter("evaluatorName"));
        if (evaluatorName == null) {
            evaluatorName = user.getDisplayName();
        }
        LocalDate assessedOn = WebSupport.parseDateOrNull(req.getParameter("assessedOn"));
        if (assessedOn == null) {
            assessedOn = LocalDate.now(container.getClock());
        }

        String brewSessionId = WebSupport.trimOrNull(req.getParameter("brewSessionId"));
        String coffeeLotId = WebSupport.trimOrNull(req.getParameter("coffeeLotId"));
        String roastBatchId = WebSupport.trimOrNull(req.getParameter("roastBatchId"));

        if (brewSessionId != null) {
            BrewSession bs = container.getBrewSessionRepository().findById(user.getId(), brewSessionId);
            if (bs != null) {
                if (coffeeLotId == null) {
                    coffeeLotId = bs.getCoffeeLotId();
                }
                if (roastBatchId == null) {
                    roastBatchId = bs.getRoastBatchId();
                }
            }
        }
        if (roastBatchId != null && coffeeLotId == null) {
            RoastBatch rb = container.getRoastBatchRepository().findById(user.getId(), roastBatchId);
            if (rb != null) {
                coffeeLotId = rb.getCoffeeLotId();
            }
        }

        String attributeScale = WebSupport.trimOrNull(req.getParameter("attributeScale"));
        if (attributeScale == null) {
            attributeScale = "1-5";
        }

        Double fragranceAroma = safeDouble(req.getParameter("fragranceAroma"));
        Double flavor = safeDouble(req.getParameter("flavor"));
        Double acidity = safeDouble(req.getParameter("acidity"));
        Double sweetness = safeDouble(req.getParameter("sweetness"));
        Double bitterness = safeDouble(req.getParameter("bitterness"));
        Double bodyTexture = safeDouble(req.getParameter("bodyTexture"));
        Double aftertaste = safeDouble(req.getParameter("aftertaste"));
        Double balance = safeDouble(req.getParameter("balance"));
        String descriptiveNotes = WebSupport.trimOrNull(req.getParameter("descriptiveNotes"));

        String assessmentId = Ids.newId();
        String descriptorScale = WebSupport.trimOrNull(req.getParameter("descriptorScale"));
        if (descriptorScale == null) {
            descriptorScale = "1-5";
        }

        List<AssessmentDescriptor> selectedDescriptors = new ArrayList<>();
        String[] descriptorIds = req.getParameterValues("descriptorIds");
        if (descriptorIds != null) {
            for (String dId : descriptorIds) {
                String cleanId = WebSupport.trimOrNull(dId);
                if (cleanId != null) {
                    Double intensity = safeDouble(req.getParameter("intensity_" + cleanId));
                    String note = WebSupport.trimOrNull(req.getParameter("note_" + cleanId));
                    selectedDescriptors.add(new AssessmentDescriptor(
                            Ids.newId(), assessmentId, cleanId, intensity, descriptorScale, note, null
                    ));
                }
            }
        }

        // Bloque afectivo estrictamente separado (PreferenceRecord)
        Double overallLiking = safeDouble(req.getParameter("overallLiking"));
        String likingScale = WebSupport.trimOrNull(req.getParameter("likingScale"));
        if (likingScale == null) {
            likingScale = "1-9";
        }
        String wouldRepeat = WebSupport.trimOrNull(req.getParameter("wouldRepeat"));
        String affectiveContext = WebSupport.trimOrNull(req.getParameter("affectiveContext"));
        String affectiveNotes = WebSupport.trimOrNull(req.getParameter("affectiveNotes"));

        PreferenceRecord preferenceRecord = null;
        if (overallLiking != null || wouldRepeat != null || affectiveContext != null || affectiveNotes != null) {
            preferenceRecord = new PreferenceRecord(
                    Ids.newId(), assessmentId, user.getId(), overallLiking, likingScale,
                    wouldRepeat, affectiveContext, affectiveNotes, now
            );
        }

        SensoryAssessment assessment = new SensoryAssessment(
                assessmentId, user.getId(), coffeeLotId, roastBatchId, brewSessionId,
                evaluatorName, assessedOn, attributeScale,
                fragranceAroma, flavor, acidity, sweetness, bitterness,
                bodyTexture, aftertaste, balance, descriptiveNotes, false,
                selectedDescriptors, preferenceRecord, now, now
        );

        container.getSensoryRepository().createAssessment(assessment);
        WebSupport.redirectWithFlash(req, resp, "/app/catacion", "success",
                "Evaluación sensorial descriptiva y registro afectivo guardados correctamente.");
    }

    private void handleCreateCustomDescriptor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String family = WebSupport.trimOrNull(req.getParameter("family"));
        String subfamily = WebSupport.trimOrNull(req.getParameter("subfamily"));
        String term = WebSupport.trimOrNull(req.getParameter("term"));
        String colorHex = WebSupport.trimOrNull(req.getParameter("colorHex"));
        if (colorHex == null || !colorHex.startsWith("#")) {
            colorHex = "#A45A2A";
        }

        if (family == null || subfamily == null || term == null) {
            WebSupport.redirectWithFlash(req, resp, "/app/catacion", "error",
                    "Indica familia, subfamilia y término para registrar el descriptor personalizado.");
            return;
        }

        SensoryDescriptor custom = new SensoryDescriptor(
                Ids.newId(), user.getId(), family, subfamily, term, colorHex,
                OriginalFlavorTaxonomy.VERSION, true, Instant.now(container.getClock())
        );
        container.getSensoryRepository().createCustomDescriptor(custom);
        WebSupport.redirectWithFlash(req, resp, "/app/catacion", "success",
                "Descriptor personalizado «" + term + "» añadido a tu taxonomía sensorial.");
    }

    private void handleDeleteAssessment(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        String assessmentId = WebSupport.trimOrNull(req.getParameter("assessmentId"));
        if (assessmentId != null) {
            container.getSensoryRepository().deleteAssessment(user.getId(), assessmentId);
        }
        WebSupport.redirectWithFlash(req, resp, "/app/catacion", "info", "Evaluación sensorial eliminada.");
    }

    private static Double safeDouble(String raw) {
        try {
            return WebSupport.parseDoubleOrNull(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
