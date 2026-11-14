package com.brewlab.domain.model;

import com.brewlab.domain.calc.RoastAge;
import com.brewlab.domain.format.Formats;

import java.time.Instant;
import java.time.LocalDate;

/** Lote o evento individual de tostión asociado a un {@link CoffeeLot} ({@code RoastBatch}). */
public final class RoastBatch {
    private final String id;
    private final String userId;
    private final String coffeeLotId;
    private final String batchCode;
    private final LocalDate roastDate;
    private final String roastLevel;
    private final String roaster;
    private final String machine;
    private final String roastNotes;
    private final Double weightLossPct;
    private final Double agtron;
    private final LocalDate openedOn;
    private final LocalDate preferredByDate;
    private final Double availableWeightG;
    private final boolean demo;
    private final Instant archivedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public RoastBatch(String id, String userId, String coffeeLotId, String batchCode,
                      LocalDate roastDate, String roastLevel, String roaster, String machine,
                      String roastNotes, Double weightLossPct, Double agtron,
                      LocalDate openedOn, LocalDate preferredByDate, Double availableWeightG,
                      boolean demo, Instant archivedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.coffeeLotId = coffeeLotId;
        this.batchCode = batchCode;
        this.roastDate = roastDate;
        this.roastLevel = roastLevel;
        this.roaster = roaster;
        this.machine = machine;
        this.roastNotes = roastNotes;
        this.weightLossPct = weightLossPct;
        this.agtron = agtron;
        this.openedOn = openedOn;
        this.preferredByDate = preferredByDate;
        this.availableWeightG = availableWeightG;
        this.demo = demo;
        this.archivedAt = archivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getCoffeeLotId() {
        return coffeeLotId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public String getCode() {
        return batchCode;
    }

    public LocalDate getRoastDate() {
        return roastDate;
    }

    public String getRoastLevel() {
        return roastLevel;
    }

    public String getRoaster() {
        return roaster;
    }

    public String getMachine() {
        return machine;
    }

    public String getRoastNotes() {
        return roastNotes;
    }

    public Double getWeightLossPct() {
        return weightLossPct;
    }

    public Double getAgtron() {
        return agtron;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }

    public LocalDate getPreferredByDate() {
        return preferredByDate;
    }

    public Double getAvailableWeightG() {
        return availableWeightG;
    }

    public boolean isDemo() {
        return demo;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Calcula dinámicamente la edad desde tostión frente a {@code today}. Nunca se almacena fija. */
    public RoastAge ageAt(LocalDate today) {
        return RoastAge.between(roastDate, today);
    }

    public String getRoastDateDisplay() {
        return Formats.date(roastDate);
    }

    public String getWeightLossDisplay() {
        return Formats.withUnit(weightLossPct, 1, "%");
    }

    public String getAgtronDisplay() {
        return Formats.number(agtron, 1);
    }

    public String getAvailableWeightDisplay() {
        return Formats.withUnit(availableWeightG, 1, "g");
    }
}
