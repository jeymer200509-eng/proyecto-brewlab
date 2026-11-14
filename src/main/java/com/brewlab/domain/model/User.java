package com.brewlab.domain.model;

import java.time.Instant;

/** Persona usuaria de BrewLab. También actúa como evaluador por defecto en Sensory Lab. */
public final class User {
    private final String id;
    private final String email;
    private final String displayName;
    private final String passwordHash;
    private final int tokenVersion;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant lastLoginAt;

    public User(String id, String email, String displayName, String passwordHash, int tokenVersion,
                Instant createdAt, Instant updatedAt, Instant lastLoginAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.tokenVersion = tokenVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLoginAt = lastLoginAt;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** No exponer en vistas. */
    public String getPasswordHash() {
        return passwordHash;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    /** Inicial para el avatar de la barra lateral. */
    public String getInitial() {
        String source = displayName == null || displayName.isEmpty() ? email : displayName;
        return source == null || source.isEmpty() ? "?" : source.substring(0, 1).toUpperCase();
    }
}
