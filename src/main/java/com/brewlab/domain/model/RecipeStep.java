package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

/** Paso temporizado dentro de una receta ({@code RecipeStep}). */
public final class RecipeStep {
    private final String id;
    private final String recipeId;
    private final int order;
    private final String title;
    private final String instruction;
    private final int startSeconds;
    private final Integer durationSeconds;
    private final Double waterDoseG;
    private final Double cumulativeWaterG;
    private final Integer toleranceSeconds;

    public RecipeStep(String id, String recipeId, int order, String title, String instruction,
                      int startSeconds, Integer durationSeconds, Double waterDoseG,
                      Double cumulativeWaterG, Integer toleranceSeconds) {
        this.id = id;
        this.recipeId = recipeId;
        this.order = order;
        this.title = title;
        this.instruction = instruction;
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;
        this.waterDoseG = waterDoseG;
        this.cumulativeWaterG = cumulativeWaterG;
        this.toleranceSeconds = toleranceSeconds;
    }

    public String getId() {
        return id;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public int getOrder() {
        return order;
    }

    public String getTitle() {
        return title;
    }

    public String getInstruction() {
        return instruction;
    }

    public int getStartSeconds() {
        return startSeconds;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Double getWaterDoseG() {
        return waterDoseG;
    }

    public Double getCumulativeWaterG() {
        return cumulativeWaterG;
    }

    public Integer getToleranceSeconds() {
        return toleranceSeconds;
    }

    public String getStartFormatted() {
        return Formats.duration(Integer.valueOf(startSeconds));
    }

    public String getDurationFormatted() {
        return Formats.duration(durationSeconds);
    }

    public String getWaterDoseFormatted() {
        return Formats.withUnit(waterDoseG, 1, "g");
    }

    public String getCumulativeWaterFormatted() {
        return Formats.withUnit(cumulativeWaterG, 1, "g");
    }
}
