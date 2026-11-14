package com.brewlab.domain.model;

import com.brewlab.domain.calc.BrewRatio;
import com.brewlab.domain.format.Formats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Receta reutilizable con parámetros previstos y lista ordenada de pasos ({@code Recipe}). */
public final class Recipe {
    private final String id;
    private final String userId;
    private final String name;
    private final String method;
    private final String coffeeLotId;
    private final String roastBatchId;
    private final Double doseG;
    private final Double waterG;
    private final Double temperatureC;
    private final String grindSetting;
    private final Integer targetTimeMinS;
    private final Integer targetTimeMaxS;
    private final List<String> equipmentIds;
    private final String waterProfileId;
    private final String notes;
    private final int version;
    private final boolean demo;
    private final List<RecipeStep> steps;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Recipe(String id, String userId, String name, String method,
                  String coffeeLotId, String roastBatchId,
                  Double doseG, Double waterG, Double temperatureC, String grindSetting,
                  Integer targetTimeMinS, Integer targetTimeMaxS,
                  List<String> equipmentIds, String waterProfileId,
                  String notes, int version, boolean demo,
                  List<RecipeStep> steps, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.method = method;
        this.coffeeLotId = coffeeLotId;
        this.roastBatchId = roastBatchId;
        this.doseG = doseG;
        this.waterG = waterG;
        this.temperatureC = temperatureC;
        this.grindSetting = grindSetting;
        this.targetTimeMinS = targetTimeMinS;
        this.targetTimeMaxS = targetTimeMaxS;
        this.equipmentIds = equipmentIds == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(equipmentIds));
        this.waterProfileId = waterProfileId;
        this.notes = notes;
        this.version = version;
        this.demo = demo;
        this.steps = steps == null
                ? Collections.<RecipeStep>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(steps));
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public String getCoffeeLotId() {
        return coffeeLotId;
    }

    public String getRoastBatchId() {
        return roastBatchId;
    }

    public Double getDoseG() {
        return doseG;
    }

    public Double getWaterG() {
        return waterG;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public String getGrindSetting() {
        return grindSetting;
    }

    public Integer getTargetTimeMinS() {
        return targetTimeMinS;
    }

    public Integer getTargetTimeMaxS() {
        return targetTimeMaxS;
    }

    public List<String> getEquipmentIds() {
        return equipmentIds;
    }

    public String getWaterProfileId() {
        return waterProfileId;
    }

    public String getNotes() {
        return notes;
    }

    public int getVersion() {
        return version;
    }

    public boolean isDemo() {
        return demo;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Ratio determinista 1:x = agua ÷ dosis. */
    public BrewRatio getRatio() {
        return BrewRatio.of(waterG, doseG);
    }

    public String getRatioDisplay() {
        String r = getRatio().format(Formats.LOCALE);
        return r != null ? r : Formats.UNKNOWN;
    }

    public String getDoseDisplay() {
        return Formats.withUnit(doseG, 1, "g");
    }

    public String getWaterDisplay() {
        return Formats.withUnit(waterG, 0, "g");
    }

    public String getTemperatureDisplay() {
        return Formats.withUnit(temperatureC, 1, "°C");
    }

    public String getTargetTimeDisplay() {
        if (targetTimeMinS == null && targetTimeMaxS == null) {
            return Formats.UNKNOWN;
        }
        if (targetTimeMinS != null && targetTimeMaxS != null && !targetTimeMinS.equals(targetTimeMaxS)) {
            return Formats.duration(targetTimeMinS) + "–" + Formats.duration(targetTimeMaxS);
        }
        Integer v = targetTimeMinS != null ? targetTimeMinS : targetTimeMaxS;
        return Formats.duration(v);
    }
}
