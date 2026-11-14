package com.brewlab.domain.model;

import java.time.Instant;

/**
 * Descriptor sensorial jerárquico (Familia → Subfamilia → Descriptor) perteneciente a la
 * taxonomía propia versionada de BrewLab o creado como descriptor personalizado del usuario.
 */
public final class SensoryDescriptor {
    public static final String TAXONOMY_VERSION_V1 = "brewlab-v1.0";

    private final String id;
    private final String userId;
    private final String family;
    private final String subfamily;
    private final String term;
    private final String colorHex;
    private final String taxonomyVersion;
    private final boolean custom;
    private final Instant createdAt;

    public SensoryDescriptor(String id, String userId, String family, String subfamily,
                             String term, String colorHex, String taxonomyVersion,
                             boolean custom, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.family = family;
        this.subfamily = subfamily;
        this.term = term;
        this.colorHex = colorHex;
        this.taxonomyVersion = taxonomyVersion;
        this.custom = custom;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getFamily() {
        return family;
    }

    public String getSubfamily() {
        return subfamily;
    }

    public String getTerm() {
        return term;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getTaxonomyVersion() {
        return taxonomyVersion;
    }

    public boolean isCustom() {
        return custom;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getHierarchyLabel() {
        return family + " › " + subfamily + " › " + term;
    }
}
