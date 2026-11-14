package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

import java.time.Instant;

/** Evento registrado durante una sesión de preparación ({@code BrewEvent}). */
public final class BrewEvent {
    public static final String TYPE_STARTED = "SESSION_STARTED";
    public static final String TYPE_PAUSED = "SESSION_PAUSED";
    public static final String TYPE_RESUMED = "SESSION_RESUMED";
    public static final String TYPE_STEP_COMPLETED = "STEP_COMPLETED";
    public static final String TYPE_STEP_SKIPPED = "STEP_SKIPPED";
    public static final String TYPE_COMPLETED = "SESSION_COMPLETED";
    public static final String TYPE_CANCELLED = "SESSION_CANCELLED";
    public static final String TYPE_CORRECTION = "SESSION_CORRECTION";

    private final String id;
    private final String brewSessionId;
    private final String recipeStepId;
    private final Integer stepOrder;
    private final String eventType;
    private final Instant occurredAt;
    private final long elapsedMs;
    private final Double targetValue;
    private final Double actualValue;
    private final String idempotencyKey;
    private final String notes;

    public BrewEvent(String id, String brewSessionId, String recipeStepId, Integer stepOrder,
                     String eventType, Instant occurredAt, long elapsedMs,
                     Double targetValue, Double actualValue, String idempotencyKey, String notes) {
        this.id = id;
        this.brewSessionId = brewSessionId;
        this.recipeStepId = recipeStepId;
        this.stepOrder = stepOrder;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.elapsedMs = elapsedMs;
        this.targetValue = targetValue;
        this.actualValue = actualValue;
        this.idempotencyKey = idempotencyKey;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public String getBrewSessionId() {
        return brewSessionId;
    }

    public String getRecipeStepId() {
        return recipeStepId;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public int getElapsedSeconds() {
        return (int) Math.round(elapsedMs / 1000.0);
    }

    public String getElapsedFormatted() {
        return Formats.duration(Integer.valueOf(getElapsedSeconds()));
    }

    public Double getTargetValue() {
        return targetValue;
    }

    public Double getActualValue() {
        return actualValue;
    }

    public String getTargetFormatted() {
        return Formats.withUnit(targetValue, 1, "g");
    }

    public String getActualFormatted() {
        return Formats.withUnit(actualValue, 1, "g");
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getNotes() {
        return notes;
    }

    public String getTypeLabel() {
        if (TYPE_STARTED.equals(eventType)) return "Inicio de preparación";
        if (TYPE_PAUSED.equals(eventType)) return "Pausa";
        if (TYPE_RESUMED.equals(eventType)) return "Reanudación";
        if (TYPE_STEP_COMPLETED.equals(eventType)) return "Etapa completada";
        if (TYPE_STEP_SKIPPED.equals(eventType)) return "Etapa omitida (confirmada)";
        if (TYPE_COMPLETED.equals(eventType)) return "Finalización de sesión";
        if (TYPE_CANCELLED.equals(eventType)) return "Sesión cancelada";
        if (TYPE_CORRECTION.equals(eventType)) return "Corrección auditable";
        return eventType;
    }
}
