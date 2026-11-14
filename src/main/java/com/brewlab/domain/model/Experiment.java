package com.brewlab.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Experimento estructurado de preparación ({@code Experiment}): pregunta de investigación,
 * objetivo, hipótesis, variable principal modificada, variables controladas, ensayos
 * ({@link ExperimentTrial}) vinculados a {@link BrewSession} y conclusión del usuario.
 */
public final class Experiment {
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CONCLUDED = "CONCLUDED";

    private final String id;
    private final String userId;
    private final String coffeeLotId;
    private final String roastBatchId;
    private final String title;
    private final String question;
    private final String objective;
    private final String hypothesis;
    private final String modifiedVariable;
    private final String controlledVariables;
    private final String status;
    private final String userConclusion;
    private final boolean demo;
    private final List<ExperimentTrial> trials;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Experiment(String id, String userId, String coffeeLotId, String roastBatchId,
                      String title, String question, String objective, String hypothesis,
                      String modifiedVariable, String controlledVariables, String status,
                      String userConclusion, boolean demo, List<ExperimentTrial> trials,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.coffeeLotId = coffeeLotId;
        this.roastBatchId = roastBatchId;
        this.title = title;
        this.question = question;
        this.objective = objective;
        this.hypothesis = hypothesis;
        this.modifiedVariable = modifiedVariable;
        this.controlledVariables = controlledVariables;
        this.status = status != null ? status : STATUS_OPEN;
        this.userConclusion = userConclusion;
        this.demo = demo;
        this.trials = trials == null
                ? Collections.<ExperimentTrial>emptyList()
                : Collections.unmodifiableList(trials);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCoffeeLotId() { return coffeeLotId; }
    public String getRoastBatchId() { return roastBatchId; }
    public String getTitle() { return title; }
    public String getQuestion() { return question; }
    public String getObjective() { return objective; }
    public String getHypothesis() { return hypothesis; }
    public String getModifiedVariable() { return modifiedVariable; }
    public String getControlledVariables() { return controlledVariables; }
    public String getStatus() { return status; }
    public String getUserConclusion() { return userConclusion; }
    public boolean isDemo() { return demo; }
    public List<ExperimentTrial> getTrials() { return trials; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getStatusLabel() {
        return STATUS_CONCLUDED.equals(status) ? "Concluido" : "En curso";
    }
}
