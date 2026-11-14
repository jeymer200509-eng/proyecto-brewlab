package com.brewlab.domain.analytics;

import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.SensoryAssessment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motor determinista y explicable basado en reglas ({@code RuleRecommendationEngine}) para
 * sugerir hipótesis de ajuste y siguientes pruebas experimentales.
 *
 * <p>Regla de dominio obligatoria: toda recomendación se etiqueta explícitamente como
 * <strong>sugerencia orientativa / hipótesis de trabajo</strong>, nunca como causa probada.
 */
public final class RuleRecommendationEngine {

    public static final class RuleSuggestion {
        private final String ruleCode;
        private final String title;
        private final String targetVariable;
        private final String observation;
        private final String suggestedHypothesis;
        private final String suggestedNextAction;

        public RuleSuggestion(String ruleCode, String title, String targetVariable,
                              String observation, String suggestedHypothesis, String suggestedNextAction) {
            this.ruleCode = ruleCode;
            this.title = title;
            this.targetVariable = targetVariable;
            this.observation = observation;
            this.suggestedHypothesis = suggestedHypothesis;
            this.suggestedNextAction = suggestedNextAction;
        }

        public String getRuleCode() { return ruleCode; }
        public String getTitle() { return title; }
        public String getTargetVariable() { return targetVariable; }
        public String getObservation() { return observation; }
        public String getSuggestedHypothesis() { return suggestedHypothesis; }
        public String getSuggestedNextAction() { return suggestedNextAction; }
    }

    private RuleRecommendationEngine() {
    }

    /**
     * Evalúa una sesión de preparación y su evaluación sensorial opcional mediante reglas
     * deterministas y explicables.
     */
    public static List<RuleSuggestion> evaluate(BrewSession session, SensoryAssessment assessment) {
        if (session == null) {
            return Collections.emptyList();
        }
        List<RuleSuggestion> suggestions = new ArrayList<>();

        Integer totalS = session.getTotalTimeSeconds();
        Integer minS = session.getPlannedTargetTimeMinS();
        Integer maxS = session.getPlannedTargetTimeMaxS();

        double scaleMax = 5.0;
        if (assessment != null && "0-10".equals(assessment.getAttributeScale())) {
            scaleMax = 10.0;
        } else if (assessment != null && "1-15".equals(assessment.getAttributeScale())) {
            scaleMax = 15.0;
        }
        double highThreshold = scaleMax * 0.72; // p. ej. >= 3.6 sobre 5
        double lowThreshold = scaleMax * 0.45;  // p. ej. <= 2.25 sobre 5

        // Regla R-01: Tiempo superior al rango objetivo + amargor elevado
        boolean timeOverMax = totalS != null && maxS != null && totalS.intValue() > maxS.intValue() + 10;
        boolean highBitterness = assessment != null && assessment.getBitterness() != null
                && assessment.getBitterness().doubleValue() >= highThreshold;

        if (timeOverMax && highBitterness) {
            suggestions.add(new RuleSuggestion(
                    "R-01",
                    "Hipótesis de drenaje lento y amargor elevado",
                    "Molienda (Grind Setting)",
                    "El tiempo total (" + totalS + " s) superó el máximo objetivo (" + maxS
                            + " s) y el amargor registrado es alto (" + assessment.getBitternessDisplay()
                            + " / " + assessment.getAttributeScale() + ").",
                    "Hipótesis sugerida: una molienda ligeramente más gruesa o menor agitación reducirá la resistencia hidráulica de la cama y atenuará compuestos amargos tardíos.",
                    "Mantén fijos dosis, agua y temperatura; abre la molienda 1 a 2 clics en el siguiente ensayo."
            ));
        } else if (timeOverMax) {
            suggestions.add(new RuleSuggestion(
                    "R-02",
                    "Hipótesis de tiempo de contacto prolongado respecto al objetivo",
                    "Molienda o caudal de vertido",
                    "El tiempo medido (" + totalS + " s) excedió el límite superior previsto (" + maxS + " s).",
                    "Hipótesis sugerida: si la taza presenta sequedad en el posgusto, engrosar un paso la molienda acercará el drenaje a la ventana prevista.",
                    "Ejecuta un ensayo cambiando únicamente el ajuste de molienda (+1 o +2 clics)."
            ));
        }

        // Regla R-03: Tiempo inferior al mínimo objetivo + acidez alta y dulzor bajo
        boolean timeUnderMin = totalS != null && minS != null && totalS.intValue() < minS.intValue() - 10;
        boolean highAcidity = assessment != null && assessment.getAcidity() != null
                && assessment.getAcidity().doubleValue() >= highThreshold;
        boolean lowSweetness = assessment != null && assessment.getSweetness() != null
                && assessment.getSweetness().doubleValue() <= lowThreshold;

        if (timeUnderMin || (highAcidity && lowSweetness)) {
            suggestions.add(new RuleSuggestion(
                    "R-03",
                    "Hipótesis de contacto corto o extracción incompleta de azúcares",
                    "Molienda o Temperatura (°C)",
                    (timeUnderMin ? "El tiempo medido (" + totalS + " s) quedó por debajo del mínimo objetivo (" + minS + " s). " : "")
                            + (highAcidity && lowSweetness ? "Se registró acidez alta con dulzor bajo." : ""),
                    "Hipótesis sugerida: afinar ligeramente la molienda o incrementar 1,5 °C la temperatura aumentará la transferencia de sólidos solubles dulces y redondeará la acidez.",
                    "Cambia una sola variable en el siguiente ensayo: afina 1 a 2 clics la molienda manteniendo el ratio y la temperatura constantes."
            ));
        }

        // Regla R-04: Cuerpo bajo percibido
        boolean lowBody = assessment != null && assessment.getBodyTexture() != null
                && assessment.getBodyTexture().doubleValue() <= lowThreshold;
        if (lowBody) {
            suggestions.add(new RuleSuggestion(
                    "R-04",
                    "Hipótesis de concentración y textura en taza",
                    "Ratio agua:café (1:x)",
                    "El atributo de cuerpo/textura se evaluó en nivel bajo (" + assessment.getBodyTextureDisplay()
                            + " / " + assessment.getAttributeScale() + ").",
                    "Hipótesis sugerida: acortar el ratio (por ejemplo de 1:16,7 a 1:15,5 reduciendo el agua total o subiendo 1 g la dosis) incrementará la concentración de sólidos y la sensación táctil.",
                    "Crea un ensayo de experimento cuya variable modificada sea el Ratio (1:x), conservando la misma molienda y temperatura."
            ));
        }

        // Regla R-05: Desviación entre agua prevista y agua real medida
        if (session.getPlannedWaterG() != null && session.getActualWaterG() != null) {
            double diffWater = Math.abs(session.getActualWaterG().doubleValue() - session.getPlannedWaterG().doubleValue());
            if (diffWater >= 8.0) {
                suggestions.add(new RuleSuggestion(
                        "R-05",
                        "Control de repetibilidad en peso de agua vertida",
                        "Control de vertido (g)",
                        "El agua real medida (" + session.getActualWaterGDisplay()
                                + ") difiere en ≥ 8 g respecto al agua prevista (" + session.getPlannedWaterGDisplay() + ").",
                        "Hipótesis sugerida: estabilizar el peso final de vertido permitirá comparar el efecto real de otras variables sin ruido de dilución.",
                        "Repite la receta objetivo verificando el corte de vertido en balanza antes de modificar molienda o temperatura."
                ));
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add(new RuleSuggestion(
                    "R-00",
                    "Parámetros dentro de rango — Exploración controlada de una variable",
                    "Molienda o Temperatura (°C)",
                    "La sesión se mantuvo cercana a los parámetros objetivo previstos.",
                    "Hipótesis sugerida: para explorar el potencial del lote sin perder trazabilidad, diseña un experimento A/B modificando únicamente una variable principal (p. ej. ±1,5 °C en temperatura o ±2 clics en molienda).",
                    "Registra dos sesiones vinculadas a un Experimento mantendo fijos lote, tueste, dosis y agua."
            ));
        }

        return Collections.unmodifiableList(suggestions);
    }
}
