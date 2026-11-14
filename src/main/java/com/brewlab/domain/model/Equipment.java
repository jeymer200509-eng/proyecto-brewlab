package com.brewlab.domain.model;

import java.time.Instant;

/** Equipo o instrumento de preparación registrado por la persona usuaria ({@code Equipment}). */
public final class Equipment {
    private final String id;
    private final String userId;
    private final String type;
    private final String brand;
    private final String model;
    private final String name;
    private final String settingsScale;
    private final String notes;
    private final boolean demo;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Equipment(String id, String userId, String type, String brand, String model,
                     String name, String settingsScale, String notes, boolean demo,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.name = name;
        this.settingsScale = settingsScale;
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

    public String getType() {
        return type;
    }

    public String getTypeLabel() {
        if ("molino".equalsIgnoreCase(type)) return "Molino";
        if ("dripper".equalsIgnoreCase(type)) return "Dripper / Método";
        if ("filtro".equalsIgnoreCase(type)) return "Filtro";
        if ("hervidor".equalsIgnoreCase(type)) return "Hervidor";
        if ("balanza".equalsIgnoreCase(type)) return "Balanza";
        if ("refractometro".equalsIgnoreCase(type)) return "Refractómetro";
        return type != null ? type : "Otro";
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    public String getSettingsScale() {
        return settingsScale;
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
}
