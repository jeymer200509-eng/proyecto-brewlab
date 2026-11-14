package com.brewlab.domain.analytics;

import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.SensoryAssessment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motor determinista de comparación entre dos o más {@link BrewSession}.
 *
 * <p>Reglas de dominio:
 * <ul>
 *   <li>Separa parámetros previstos, reales medidos, descriptivos sensoriales y agrado afectivo.</li>
 *   <li>Conserva {@code null} como {@code "—"} (nunca {@code 0}).</li>
 *   <li>Informa tamaño de muestra {@code N}, unidades y diferencias de contexto.</li>
 *   <li>Advierte explícitamente cuando cambian múltiples variables simultáneamente para evitar
 *       afirmar causalidad a partir de correlación.</li>
 * </ul>
 */
public final class SessionComparison {

    public static final class ComparedItem {
        private final BrewSession session;
        private final SensoryAssessment assessment;
        private final String colorHex;

        public ComparedItem(BrewSession session, SensoryAssessment assessment, String colorHex) {
            this.session = session;
            this.assessment = assessment;
            this.colorHex = colorHex;
        }

        public BrewSession getSession() { return session; }
        public SensoryAssessment getAssessment() { return assessment; }
        public String getColorHex() { return colorHex; }

        public int getSvgBarHeight(Double value, double scaleMax) {
            if (value == null || scaleMax <= 0.0) {
                return 0;
            }
            double clamped = Math.max(0.0, Math.min(scaleMax, value.doubleValue()));
            return (int) Math.round((clamped / scaleMax) * 140.0);
        }
    }

    private static final String[] PALETTE = {
            "#A45A2A", "#2C6E63", "#5B5EA6", "#C86D2D", "#8B4F7D", "#3B7A57"
    };

    private final List<ComparedItem> items;
    private final List<String> changedVariables;
    private final List<String> contextWarnings;
    private final double sensoryChartMax;

    private SessionComparison(List<ComparedItem> items, List<String> changedVariables,
                              List<String> contextWarnings, double sensoryChartMax) {
        this.items = Collections.unmodifiableList(items);
        this.changedVariables = Collections.unmodifiableList(changedVariables);
        this.contextWarnings = Collections.unmodifiableList(contextWarnings);
        this.sensoryChartMax = sensoryChartMax;
    }

    public static SessionComparison build(List<BrewSession> sessions,
                                          Map<String, SensoryAssessment> latestAssessmentBySessionId) {
        if (sessions == null || sessions.isEmpty()) {
            return new SessionComparison(
                    Collections.<ComparedItem>emptyList(),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    5.0
            );
        }

        List<ComparedItem> items = new ArrayList<>();
        Set<String> lots = new LinkedHashSet<>();
        Set<String> batches = new LinkedHashSet<>();
        Set<String> methods = new LinkedHashSet<>();
        Set<String> doses = new LinkedHashSet<>();
        Set<String> waters = new LinkedHashSet<>();
        Set<String> temps = new LinkedHashSet<>();
        Set<String> grinds = new LinkedHashSet<>();
        Set<String> waterProfiles = new LinkedHashSet<>();
        Set<String> scales = new LinkedHashSet<>();
        double maxScale = 5.0;

        for (int i = 0; i < sessions.size(); i++) {
            BrewSession s = sessions.get(i);
            SensoryAssessment a = latestAssessmentBySessionId != null
                    ? latestAssessmentBySessionId.get(s.getId())
                    : null;
            String color = PALETTE[i % PALETTE.length];
            items.add(new ComparedItem(s, a, color));

            lots.add(safeKey(s.getCoffeeLotId()));
            batches.add(safeKey(s.getRoastBatchId()));
            methods.add(safeKey(s.getPlannedMethod()));
            doses.add(Formats.number(s.getActualDoseG() != null ? s.getActualDoseG() : s.getPlannedDoseG(), 1));
            waters.add(Formats.number(s.getActualWaterG() != null ? s.getActualWaterG() : s.getPlannedWaterG(), 1));
            temps.add(Formats.number(s.getActualTemperatureC() != null ? s.getActualTemperatureC() : s.getPlannedTemperatureC(), 1));
            grinds.add(safeKey(s.getActualGrindSetting() != null ? s.getActualGrindSetting() : s.getPlannedGrindSetting()));
            waterProfiles.add(safeKey(s.getWaterProfileId()));

            if (a != null && a.getAttributeScale() != null) {
                scales.add(a.getAttributeScale());
                if ("0-10".equals(a.getAttributeScale())) {
                    maxScale = Math.max(maxScale, 10.0);
                } else if ("1-15".equals(a.getAttributeScale())) {
                    maxScale = Math.max(maxScale, 15.0);
                }
            }
        }

        List<String> changed = new ArrayList<>();
        if (lots.size() > 1) changed.add("Lote de café (CoffeeLot)");
        if (batches.size() > 1) changed.add("Lote de tostión (RoastBatch)");
        if (methods.size() > 1) changed.add("Método de extracción");
        if (doses.size() > 1) changed.add("Dosis de café (g)");
        if (waters.size() > 1) changed.add("Agua total (g)");
        if (temps.size() > 1) changed.add("Temperatura (°C)");
        if (grinds.size() > 1) changed.add("Ajuste de molienda");
        if (waterProfiles.size() > 1) changed.add("Perfil de agua");

        List<String> warnings = new ArrayList<>();
        if (lots.size() > 1) {
            warnings.add("Las sesiones comparadas pertenecen a distintos lotes de café (CoffeeLot). Las diferencias sensoriales reflejan materia prima distinta además de la receta.");
        }
        if (batches.size() > 1) {
            warnings.add("Las sesiones comparadas usan distintos lotes de tostión (RoastBatch). Ten en cuenta la edad desde tostión y el nivel de tueste antes de atribuir cambios a la extracción.");
        }
        if (changed.size() > 1) {
            warnings.add("Se detectaron " + changed.size() + " variables cambiadas simultáneamente ("
                    + String.join(", ", changed)
                    + "). Correlación no implica causalidad: para aislar un efecto causal, modifica una sola variable principal por ensayo en Experiment Lab.");
        }
        if (scales.size() > 1) {
            warnings.add("Atención metodológica: las evaluaciones sensoriales vinculadas usan escalas distintas ("
                    + String.join(" vs ", scales) + "). Interpreta los valores considerando la escala declarada de cada sesión.");
        }

        return new SessionComparison(items, changed, warnings, maxScale);
    }

    public List<ComparedItem> getItems() {
        return items;
    }

    public int getSampleSize() {
        return items.size();
    }

    public List<String> getChangedVariables() {
        return changedVariables;
    }

    public List<String> getContextWarnings() {
        return contextWarnings;
    }

    public double getSensoryChartMax() {
        return sensoryChartMax;
    }

    private static String safeKey(String s) {
        return s == null || s.trim().isEmpty() ? "__NULL__" : s.trim();
    }
}
