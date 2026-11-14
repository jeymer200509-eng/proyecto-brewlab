package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

import java.time.Instant;
import java.time.LocalDate;

/** Perfil de agua de preparación ({@code WaterProfile}). Datos no medidos permanecen {@code null}. */
public final class WaterProfile {
    private final String id;
    private final String userId;
    private final String name;
    private final String source;
    private final Double tdsPpm;
    private final Double hardnessGh;
    private final Double alkalinityKh;
    private final String minerals;
    private final LocalDate measuredAt;
    private final String notes;
    private final boolean demo;
    private final Instant createdAt;
    private final Instant updatedAt;

    public WaterProfile(String id, String userId, String name, String source,
                        Double tdsPpm, Double hardnessGh, Double alkalinityKh,
                        String minerals, LocalDate measuredAt, String notes,
                        boolean demo, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.source = source;
        this.tdsPpm = tdsPpm;
        this.hardnessGh = hardnessGh;
        this.alkalinityKh = alkalinityKh;
        this.minerals = minerals;
        this.measuredAt = measuredAt;
        this.notes = notes;
        this.demo = demo;
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

    public String getSource() {
        return source;
    }

    public Double getTdsPpm() {
        return tdsPpm;
    }

    public Double getHardnessGh() {
        return hardnessGh;
    }

    public Double getAlkalinityKh() {
        return alkalinityKh;
    }

    public String getMinerals() {
        return minerals;
    }

    public LocalDate getMeasuredAt() {
        return measuredAt;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isDemo() {
        return demo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getTdsDisplay() {
        return Formats.withUnit(tdsPpm, 0, "mg/L");
    }

    public String getHardnessDisplay() {
        return Formats.withUnit(hardnessGh, 0, "mg/L CaCO₃");
    }

    public String getAlkalinityDisplay() {
        return Formats.withUnit(alkalinityKh, 0, "mg/L CaCO₃");
    }
}
