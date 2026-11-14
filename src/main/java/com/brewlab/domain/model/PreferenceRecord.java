package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

import java.time.Instant;

/**
 * Bloque afectivo separado de la evaluación descriptiva: registra agrado general,
 * intención de repetir y notas subjetivas/contextuales sin mezclarlas con la intensidad descriptiva.
 */
public final class PreferenceRecord {
    private final String id;
    private final String sensoryAssessmentId;
    private final String userId;
    private final Double overallLiking;
    private final String likingScale;
    private final String wouldRepeat;
    private final String affectiveContext;
    private final String affectiveNotes;
    private final Instant createdAt;

    public PreferenceRecord(String id, String sensoryAssessmentId, String userId,
                            Double overallLiking, String likingScale, String wouldRepeat,
                            String affectiveContext, String affectiveNotes, Instant createdAt) {
        this.id = id;
        this.sensoryAssessmentId = sensoryAssessmentId;
        this.userId = userId;
        this.overallLiking = overallLiking;
        this.likingScale = likingScale;
        this.wouldRepeat = wouldRepeat;
        this.affectiveContext = affectiveContext;
        this.affectiveNotes = affectiveNotes;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSensoryAssessmentId() {
        return sensoryAssessmentId;
    }

    public String getUserId() {
        return userId;
    }

    public Double getOverallLiking() {
        return overallLiking;
    }

    public String getLikingScale() {
        return likingScale;
    }

    public String getWouldRepeat() {
        return wouldRepeat;
    }

    public String getAffectiveContext() {
        return affectiveContext;
    }

    public String getAffectiveNotes() {
        return affectiveNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getOverallLikingDisplay() {
        return overallLiking == null ? Formats.UNKNOWN : Formats.number(overallLiking, 1) + " (" + likingScale + ")";
    }

    public String getWouldRepeatDisplay() {
        if ("YES".equalsIgnoreCase(wouldRepeat)) {
            return "Sí, repetir";
        }
        if ("NO".equalsIgnoreCase(wouldRepeat)) {
            return "No repetir";
        }
        if ("MAYBE".equalsIgnoreCase(wouldRepeat)) {
            return "Con ajustes";
        }
        return Formats.UNKNOWN;
    }
}
