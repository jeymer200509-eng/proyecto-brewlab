package com.brewlab.domain.model;

import java.time.Instant;

/**
 * Ensayo experimental ({@code ExperimentTrial}) vinculado obligatoriamente a una
 * {@link BrewSession} real para permitir contrastar el valor de la variable modificada
 * frente a los resultados medidos y sensoriales.
 */
public final class ExperimentTrial {
    private final String id;
    private final String experimentId;
    private final String brewSessionId;
    private final int trialOrder;
    private final String label;
    private final String modifiedValue;
    private final String notes;
    private final Instant createdAt;

    public ExperimentTrial(String id, String experimentId, String brewSessionId,
                           int trialOrder, String label, String modifiedValue,
                           String notes, Instant createdAt) {
        this.id = id;
        this.experimentId = experimentId;
        this.brewSessionId = brewSessionId;
        this.trialOrder = trialOrder;
        this.label = label;
        this.modifiedValue = modifiedValue;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getBrewSessionId() {
        return brewSessionId;
    }

    public int getTrialOrder() {
        return trialOrder;
    }

    public String getLabel() {
        return label;
    }

    public String getModifiedValue() {
        return modifiedValue;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
