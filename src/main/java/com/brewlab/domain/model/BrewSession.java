package com.brewlab.domain.model;

import com.brewlab.domain.calc.BrewRatio;
import com.brewlab.domain.format.Formats;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ejecución real de una preparación ({@code BrewSession}). Conserva una instantánea inmutable
 * de la receta y parámetros previstos al comenzar, separada de los parámetros reales medidos.
 */
public final class BrewSession {
    public static final String STATUS_PREPARED = "PREPARED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private static final Gson GSON = new Gson();
    private static final Type STEP_LIST_TYPE = new TypeToken<List<RecipeStep>>() {}.getType();

    private final String id;
    private final String userId;
    private final String recipeId;
    private final String coffeeLotId;
    private final String roastBatchId;
    private final String waterProfileId;
    private final List<String> equipmentIds;
    private final String status;
    private final Instant startedAt;
    private final Instant pausedAt;
    private final long accumulatedPauseMs;
    private final Instant completedAt;

    // Instantánea de parámetros previstos (inmutable aunque la receta cambie después)
    private final String plannedRecipeName;
    private final String plannedMethod;
    private final Double plannedDoseG;
    private final Double plannedWaterG;
    private final Double plannedTemperatureC;
    private final String plannedGrindSetting;
    private final Integer plannedTargetTimeMinS;
    private final Integer plannedTargetTimeMaxS;
    private final String recipeSnapshotJson;
    private final List<RecipeStep> snapshotSteps;

    // Parámetros reales medidos en la sesión (null cuando no se midieron)
    private final Double actualDoseG;
    private final Double actualWaterG;
    private final Double actualTemperatureC;
    private final String actualGrindSetting;
    private final Integer totalTimeSeconds;
    private final Double finalBeverageWeightG;
    private final String notes;
    private final boolean demo;
    private final List<BrewEvent> events;
    private final Instant createdAt;
    private final Instant updatedAt;

    public BrewSession(String id, String userId, String recipeId, String coffeeLotId,
                       String roastBatchId, String waterProfileId, List<String> equipmentIds,
                       String status, Instant startedAt, Instant pausedAt,
                       long accumulatedPauseMs, Instant completedAt,
                       String plannedRecipeName, String plannedMethod,
                       Double plannedDoseG, Double plannedWaterG, Double plannedTemperatureC,
                       String plannedGrindSetting, Integer plannedTargetTimeMinS, Integer plannedTargetTimeMaxS,
                       String recipeSnapshotJson,
                       Double actualDoseG, Double actualWaterG, Double actualTemperatureC,
                       String actualGrindSetting, Integer totalTimeSeconds, Double finalBeverageWeightG,
                       String notes, boolean demo, List<BrewEvent> events,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.recipeId = recipeId;
        this.coffeeLotId = coffeeLotId;
        this.roastBatchId = roastBatchId;
        this.waterProfileId = waterProfileId;
        this.equipmentIds = equipmentIds == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(equipmentIds));
        this.status = status;
        this.startedAt = startedAt;
        this.pausedAt = pausedAt;
        this.accumulatedPauseMs = accumulatedPauseMs;
        this.completedAt = completedAt;
        this.plannedRecipeName = plannedRecipeName;
        this.plannedMethod = plannedMethod;
        this.plannedDoseG = plannedDoseG;
        this.plannedWaterG = plannedWaterG;
        this.plannedTemperatureC = plannedTemperatureC;
        this.plannedGrindSetting = plannedGrindSetting;
        this.plannedTargetTimeMinS = plannedTargetTimeMinS;
        this.plannedTargetTimeMaxS = plannedTargetTimeMaxS;
        this.recipeSnapshotJson = recipeSnapshotJson != null ? recipeSnapshotJson : "[]";
        this.snapshotSteps = parseSnapshotSteps(this.recipeSnapshotJson);
        this.actualDoseG = actualDoseG;
        this.actualWaterG = actualWaterG;
        this.actualTemperatureC = actualTemperatureC;
        this.actualGrindSetting = actualGrindSetting;
        this.totalTimeSeconds = totalTimeSeconds;
        this.finalBeverageWeightG = finalBeverageWeightG;
        this.notes = notes;
        this.demo = demo;
        this.events = events == null
                ? Collections.<BrewEvent>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(events));
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static String serializeStepsSnapshot(List<RecipeStep> steps) {
        return GSON.toJson(steps != null ? steps : Collections.<RecipeStep>emptyList());
    }

    private static List<RecipeStep> parseSnapshotSteps(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<RecipeStep> parsed = GSON.fromJson(json, STEP_LIST_TYPE);
            return parsed != null ? Collections.unmodifiableList(parsed) : Collections.<RecipeStep>emptyList();
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getCoffeeLotId() {
        return coffeeLotId;
    }

    public String getRoastBatchId() {
        return roastBatchId;
    }

    public String getWaterProfileId() {
        return waterProfileId;
    }

    public List<String> getEquipmentIds() {
        return equipmentIds;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public boolean isPaused() {
        return STATUS_PAUSED.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isInProgress() {
        return STATUS_ACTIVE.equals(status) || STATUS_PAUSED.equals(status) || STATUS_PREPARED.equals(status);
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long getStartedAtEpochMs() {
        return startedAt != null ? startedAt.toEpochMilli() : 0L;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public long getPausedAtEpochMs() {
        return pausedAt != null ? pausedAt.toEpochMilli() : 0L;
    }

    public long getAccumulatedPauseMs() {
        return accumulatedPauseMs;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getPlannedRecipeName() {
        return plannedRecipeName;
    }

    public String getPlannedMethod() {
        return plannedMethod;
    }

    public Double getPlannedDoseG() {
        return plannedDoseG;
    }

    public Double getPlannedWaterG() {
        return plannedWaterG;
    }

    public Double getPlannedTemperatureC() {
        return plannedTemperatureC;
    }

    public String getPlannedGrindSetting() {
        return plannedGrindSetting;
    }

    public Integer getPlannedTargetTimeMinS() {
        return plannedTargetTimeMinS;
    }

    public Integer getPlannedTargetTimeMaxS() {
        return plannedTargetTimeMaxS;
    }

    public String getRecipeSnapshotJson() {
        return recipeSnapshotJson;
    }

    public List<RecipeStep> getSnapshotSteps() {
        return snapshotSteps;
    }

    public Double getActualDoseG() {
        return actualDoseG;
    }

    public Double getActualWaterG() {
        return actualWaterG;
    }

    public Double getActualTemperatureC() {
        return actualTemperatureC;
    }

    public String getActualGrindSetting() {
        return actualGrindSetting;
    }

    public Integer getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public Double getFinalBeverageWeightG() {
        return finalBeverageWeightG;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isDemo() {
        return demo;
    }

    public List<BrewEvent> getEvents() {
        return events;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public BrewRatio getPlannedRatio() {
        return BrewRatio.of(plannedWaterG, plannedDoseG);
    }

    public BrewRatio getActualRatio() {
        return BrewRatio.of(actualWaterG, actualDoseG);
    }

    public String getPlannedRatioDisplay() {
        String s = getPlannedRatio().format(Formats.LOCALE);
        return s != null ? s : Formats.UNKNOWN;
    }

    public String getActualRatioDisplay() {
        String s = getActualRatio().format(Formats.LOCALE);
        return s != null ? s : Formats.UNKNOWN;
    }

    public String getPlannedDoseDisplay() {
        return Formats.withUnit(plannedDoseG, 1, "g");
    }

    public String getActualDoseDisplay() {
        return Formats.withUnit(actualDoseG, 1, "g");
    }

    public String getPlannedWaterDisplay() {
        return Formats.withUnit(plannedWaterG, 0, "g");
    }

    public String getActualWaterDisplay() {
        return Formats.withUnit(actualWaterG, 0, "g");
    }

    public String getPlannedTempDisplay() {
        return Formats.withUnit(plannedTemperatureC, 1, "°C");
    }

    public String getActualTempDisplay() {
        return Formats.withUnit(actualTemperatureC, 1, "°C");
    }

    public String getTotalTimeDisplay() {
        return Formats.duration(totalTimeSeconds);
    }

    public String getFinalBeverageDisplay() {
        return Formats.withUnit(finalBeverageWeightG, 1, "g");
    }
}
