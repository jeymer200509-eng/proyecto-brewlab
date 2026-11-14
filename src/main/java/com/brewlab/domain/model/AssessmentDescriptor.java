package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

/**
 * Vínculo entre una evaluación sensorial y un descriptor jerárquico con su intensidad y escala
 * almacenadas por separado.
 */
public final class AssessmentDescriptor {
    private final String id;
    private final String sensoryAssessmentId;
    private final String descriptorId;
    private final Double intensity;
    private final String scale;
    private final String notes;
    private final SensoryDescriptor descriptor;

    public AssessmentDescriptor(String id, String sensoryAssessmentId, String descriptorId,
                                Double intensity, String scale, String notes,
                                SensoryDescriptor descriptor) {
        this.id = id;
        this.sensoryAssessmentId = sensoryAssessmentId;
        this.descriptorId = descriptorId;
        this.intensity = intensity;
        this.scale = scale;
        this.notes = notes;
        this.descriptor = descriptor;
    }

    public String getId() {
        return id;
    }

    public String getSensoryAssessmentId() {
        return sensoryAssessmentId;
    }

    public String getDescriptorId() {
        return descriptorId;
    }

    public Double getIntensity() {
        return intensity;
    }

    public String getScale() {
        return scale;
    }

    public String getNotes() {
        return notes;
    }

    public SensoryDescriptor getDescriptor() {
        return descriptor;
    }

    public String getIntensityDisplay() {
        return intensity == null ? Formats.UNKNOWN : Formats.number(intensity, 1) + " / " + scale;
    }
}
