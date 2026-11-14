package com.brewlab.domain.model;

import com.brewlab.domain.format.Formats;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Evaluación sensorial descriptiva de laboratorio (V1), vinculable a {@link CoffeeLot},
 * {@link RoastBatch} y opcionalmente {@link BrewSession}. Mantiene estrictamente separados
 * los atributos descriptivos orientativos y el registro afectivo ({@link PreferenceRecord}).
 *
 * <p>Nota de dominio: esta evaluación descriptiva interna de laboratorio no representa ni
 * sustituye un formulario oficial SCA/CVA.
 */
public final class SensoryAssessment {
    private final String id;
    private final String userId;
    private final String coffeeLotId;
    private final String roastBatchId;
    private final String brewSessionId;
    private final String evaluatorName;
    private final LocalDate assessedOn;
    private final String attributeScale;
    private final Double fragranceAroma;
    private final Double flavor;
    private final Double acidity;
    private final Double sweetness;
    private final Double bitterness;
    private final Double bodyTexture;
    private final Double aftertaste;
    private final Double balance;
    private final String descriptiveNotes;
    private final boolean demo;
    private final List<AssessmentDescriptor> descriptors;
    private final PreferenceRecord preferenceRecord;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SensoryAssessment(String id, String userId, String coffeeLotId, String roastBatchId,
                             String brewSessionId, String evaluatorName, LocalDate assessedOn,
                             String attributeScale, Double fragranceAroma, Double flavor,
                             Double acidity, Double sweetness, Double bitterness,
                             Double bodyTexture, Double aftertaste, Double balance,
                             String descriptiveNotes, boolean demo,
                             List<AssessmentDescriptor> descriptors,
                             PreferenceRecord preferenceRecord,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.coffeeLotId = coffeeLotId;
        this.roastBatchId = roastBatchId;
        this.brewSessionId = brewSessionId;
        this.evaluatorName = evaluatorName;
        this.assessedOn = assessedOn;
        this.attributeScale = attributeScale != null ? attributeScale : "1-5";
        this.fragranceAroma = fragranceAroma;
        this.flavor = flavor;
        this.acidity = acidity;
        this.sweetness = sweetness;
        this.bitterness = bitterness;
        this.bodyTexture = bodyTexture;
        this.aftertaste = aftertaste;
        this.balance = balance;
        this.descriptiveNotes = descriptiveNotes;
        this.demo = demo;
        this.descriptors = descriptors == null
                ? Collections.<AssessmentDescriptor>emptyList()
                : Collections.unmodifiableList(descriptors);
        this.preferenceRecord = preferenceRecord;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCoffeeLotId() { return coffeeLotId; }
    public String getRoastBatchId() { return roastBatchId; }
    public String getBrewSessionId() { return brewSessionId; }
    public String getEvaluatorName() { return evaluatorName; }
    public LocalDate getAssessedOn() { return assessedOn; }
    public String getAssessedOnDisplay() { return Formats.date(assessedOn); }
    public String getAttributeScale() { return attributeScale; }
    public Double getFragranceAroma() { return fragranceAroma; }
    public Double getFlavor() { return flavor; }
    public Double getAcidity() { return acidity; }
    public Double getSweetness() { return sweetness; }
    public Double getBitterness() { return bitterness; }
    public Double getBodyTexture() { return bodyTexture; }
    public Double getAftertaste() { return aftertaste; }
    public Double getBalance() { return balance; }
    public String getDescriptiveNotes() { return descriptiveNotes; }
    public boolean isDemo() { return demo; }
    public List<AssessmentDescriptor> getDescriptors() { return descriptors; }
    public PreferenceRecord getPreferenceRecord() { return preferenceRecord; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String getFragranceAromaDisplay() { return formatAttr(fragranceAroma); }
    public String getFlavorDisplay() { return formatAttr(flavor); }
    public String getAcidityDisplay() { return formatAttr(acidity); }
    public String getSweetnessDisplay() { return formatAttr(sweetness); }
    public String getBitternessDisplay() { return formatAttr(bitterness); }
    public String getBodyTextureDisplay() { return formatAttr(bodyTexture); }
    public String getAftertasteDisplay() { return formatAttr(aftertaste); }
    public String getBalanceDisplay() { return formatAttr(balance); }

    private String formatAttr(Double value) {
        return value == null ? Formats.UNKNOWN : Formats.number(value, 2);
    }
}
