package com.brewlab.domain.validation;

import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;
import com.brewlab.domain.model.WaterProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Validadores puros para equipos, perfiles de agua, recetas y coherencia de pasos de vertido.
 * Separa errores bloqueantes ({@link FieldErrors}) de avisos de coherencia que el usuario puede
 * revisar y corregir sin perder su configuración.
 */
public final class RecipeValidators {

    private RecipeValidators() {
    }

    public static FieldErrors validateEquipment(Equipment eq) {
        FieldErrors errors = new FieldErrors();
        if (eq.getType() == null || eq.getType().trim().isEmpty()) {
            errors.add("type", "Selecciona el tipo de equipo.");
        }
        if (eq.getName() == null || eq.getName().trim().isEmpty()) {
            errors.add("name", "El nombre identificativo del equipo es obligatorio.");
        }
        return errors;
    }

    public static FieldErrors validateWaterProfile(WaterProfile wp) {
        FieldErrors errors = new FieldErrors();
        if (wp.getName() == null || wp.getName().trim().isEmpty()) {
            errors.add("name", "El nombre del perfil de agua es obligatorio.");
        }
        if (wp.getTdsPpm() != null && wp.getTdsPpm() < 0) {
            errors.add("tdsPpm", "El TDS no puede ser negativo.");
        }
        if (wp.getHardnessGh() != null && wp.getHardnessGh() < 0) {
            errors.add("hardnessGh", "La dureza no puede ser negativa.");
        }
        if (wp.getAlkalinityKh() != null && wp.getAlkalinityKh() < 0) {
            errors.add("alkalinityKh", "La alcalinidad no puede ser negativa.");
        }
        return errors;
    }

    public static FieldErrors validateRecipe(Recipe recipe) {
        FieldErrors errors = new FieldErrors();
        if (recipe.getName() == null || recipe.getName().trim().isEmpty()) {
            errors.add("name", "El nombre de la receta es obligatorio.");
        }
        if (recipe.getMethod() == null || recipe.getMethod().trim().isEmpty()) {
            errors.add("method", "El método de preparación es obligatorio.");
        }
        if (recipe.getDoseG() == null || recipe.getDoseG() <= 0) {
            errors.add("doseG", "La dosis de café (g) debe ser un número positivo mayor que cero.");
        }
        if (recipe.getWaterG() == null || recipe.getWaterG() <= 0) {
            errors.add("waterG", "El agua total (g) debe ser un número positivo mayor que cero.");
        }
        if (recipe.getTemperatureC() != null && (recipe.getTemperatureC() <= 0 || recipe.getTemperatureC() > 105)) {
            errors.add("temperatureC", "La temperatura objetivo (°C) debe estar entre 1 y 105 °C.");
        }
        if (recipe.getTargetTimeMinS() != null && recipe.getTargetTimeMinS() < 0) {
            errors.add("targetTimeMinS", "El tiempo objetivo mínimo no puede ser negativo.");
        }
        if (recipe.getTargetTimeMaxS() != null && recipe.getTargetTimeMaxS() < 0) {
            errors.add("targetTimeMaxS", "El tiempo objetivo máximo no puede ser negativo.");
        }
        if (recipe.getTargetTimeMinS() != null && recipe.getTargetTimeMaxS() != null
                && recipe.getTargetTimeMinS() > recipe.getTargetTimeMaxS()) {
            errors.add("targetTimeMaxS", "El tiempo máximo debe ser mayor o igual al mínimo.");
        }
        return errors;
    }

    public static FieldErrors validateStep(RecipeStep step) {
        FieldErrors errors = new FieldErrors();
        if (step.getTitle() == null || step.getTitle().trim().isEmpty()) {
            errors.add("title", "El título del paso es obligatorio.");
        }
        if (step.getInstruction() == null || step.getInstruction().trim().isEmpty()) {
            errors.add("instruction", "La instrucción del paso es obligatoria.");
        }
        if (step.getStartSeconds() < 0) {
            errors.add("startSeconds", "El tiempo de inicio no puede ser negativo.");
        }
        if (step.getDurationSeconds() != null && step.getDurationSeconds() < 0) {
            errors.add("durationSeconds", "La duración del paso no puede ser negativa.");
        }
        if (step.getWaterDoseG() != null && step.getWaterDoseG() < 0) {
            errors.add("waterDoseG", "El agua del paso no puede ser negativa.");
        }
        if (step.getCumulativeWaterG() != null && step.getCumulativeWaterG() < 0) {
            errors.add("cumulativeWaterG", "El agua acumulada no puede ser negativa.");
        }
        if (step.getToleranceSeconds() != null && step.getToleranceSeconds() < 0) {
            errors.add("toleranceSeconds", "La tolerancia no puede ser negativa.");
        }
        return errors;
    }

    /**
     * Evalúa incoherencias entre la receta y sus pasos ordenados (sin bloquear una prueba manual):
     * <ul>
     *   <li>Agua acumulada que disminuye entre un paso y el siguiente.</li>
     *   <li>Tiempo de inicio regresivo entre pasos sucesivos.</li>
     *   <li>Diferencia entre suma de vertidos / último acumulado y el agua total de la receta.</li>
     *   <li>Ausencia de campos recomendados (temperatura o molienda).</li>
     * </ul>
     */
    public static List<String> inspectCoherenceWarnings(Recipe recipe) {
        List<String> warnings = new ArrayList<>();
        if (recipe.getTemperatureC() == null) {
            warnings.add("La receta no especifica temperatura objetivo (°C); se registrará como dato no previsto.");
        }
        if (recipe.getGrindSetting() == null || recipe.getGrindSetting().trim().isEmpty()) {
            warnings.add("La receta no especifica ajuste de molienda previsto.");
        }
        List<RecipeStep> steps = recipe.getSteps();
        if (steps == null || steps.isEmpty()) {
            warnings.add("La receta aún no tiene pasos temporizados definidos.");
            return warnings;
        }

        double sumDoseWater = 0.0;
        boolean anyDoseWater = false;
        Double lastCumulative = null;
        int prevStart = -1;

        for (RecipeStep step : steps) {
            if (prevStart >= 0 && step.getStartSeconds() < prevStart) {
                warnings.add("El paso #" + step.getOrder() + " («" + step.getTitle() + "») inicia en "
                        + step.getStartFormatted() + ", antes del paso anterior ("
                        + Formats.duration(Integer.valueOf(prevStart)) + ").");
            }
            prevStart = step.getStartSeconds();

            if (step.getWaterDoseG() != null) {
                sumDoseWater += step.getWaterDoseG().doubleValue();
                anyDoseWater = true;
            }
            if (step.getCumulativeWaterG() != null) {
                if (lastCumulative != null && step.getCumulativeWaterG().doubleValue() < lastCumulative.doubleValue() - 0.01) {
                    warnings.add("El agua acumulada disminuye en el paso #" + step.getOrder() + " («"
                            + step.getTitle() + "»): pasa de " + Formats.withUnit(lastCumulative, 1, "g")
                            + " a " + step.getCumulativeWaterFormatted() + ".");
                }
                lastCumulative = step.getCumulativeWaterG();
            }
        }

        if (recipe.getWaterG() != null && recipe.getWaterG() > 0) {
            double totalWater = recipe.getWaterG().doubleValue();
            if (anyDoseWater && Math.abs(sumDoseWater - totalWater) > 0.15) {
                warnings.add("La suma del agua por paso (" + Formats.withUnit(Double.valueOf(sumDoseWater), 1, "g")
                        + ") no coincide con el agua total de la receta (" + recipe.getWaterDisplay() + ").");
            }
            if (lastCumulative != null && Math.abs(lastCumulative.doubleValue() - totalWater) > 0.15) {
                warnings.add("El agua acumulada final de los pasos ("
                        + Formats.withUnit(lastCumulative, 1, "g")
                        + ") difiere del agua total de la receta (" + recipe.getWaterDisplay() + ").");
            }
        }
        return warnings;
    }
}
