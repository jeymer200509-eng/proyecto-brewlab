-- =====================================================================
-- V005__sensory_lab.sql
-- BrewLab Fase 5: Taxonomía Sensorial Propia Versionada (SensoryDescriptor),
-- Evaluación Descriptiva (SensoryAssessment + AssessmentDescriptor) y
-- Registro Afectivo Separado (PreferenceRecord)
-- =====================================================================

CREATE TABLE IF NOT EXISTS sensory_descriptors (
    id VARCHAR(40) NOT NULL,
    user_id VARCHAR(40) NULL,
    family VARCHAR(80) NOT NULL,
    subfamily VARCHAR(80) NOT NULL,
    term VARCHAR(100) NOT NULL,
    color_hex VARCHAR(16) NOT NULL,
    taxonomy_version VARCHAR(30) NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sensory_descriptors_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_sensory_descriptors_family ON sensory_descriptors (family, subfamily);
CREATE INDEX idx_sensory_descriptors_user ON sensory_descriptors (user_id);

CREATE TABLE IF NOT EXISTS sensory_assessments (
    id VARCHAR(40) NOT NULL,
    user_id VARCHAR(40) NOT NULL,
    coffee_lot_id VARCHAR(40) NULL,
    roast_batch_id VARCHAR(40) NULL,
    brew_session_id VARCHAR(40) NULL,
    evaluator_name VARCHAR(120) NOT NULL,
    assessed_on DATE NOT NULL,
    attribute_scale VARCHAR(20) NOT NULL,
    fragrance_aroma DOUBLE NULL,
    flavor DOUBLE NULL,
    acidity DOUBLE NULL,
    sweetness DOUBLE NULL,
    bitterness DOUBLE NULL,
    body_texture DOUBLE NULL,
    aftertaste DOUBLE NULL,
    balance DOUBLE NULL,
    descriptive_notes VARCHAR(1500) NULL,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sensory_assessments_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_sensory_assessments_lot FOREIGN KEY (coffee_lot_id)
        REFERENCES coffee_lots (id) ON DELETE SET NULL,
    CONSTRAINT fk_sensory_assessments_batch FOREIGN KEY (roast_batch_id)
        REFERENCES roast_batches (id) ON DELETE SET NULL,
    CONSTRAINT fk_sensory_assessments_session FOREIGN KEY (brew_session_id)
        REFERENCES brew_sessions (id) ON DELETE SET NULL
);

CREATE INDEX idx_sensory_assessments_user ON sensory_assessments (user_id, assessed_on);
CREATE INDEX idx_sensory_assessments_session ON sensory_assessments (brew_session_id);
CREATE INDEX idx_sensory_assessments_lot ON sensory_assessments (coffee_lot_id);

CREATE TABLE IF NOT EXISTS assessment_descriptors (
    id VARCHAR(40) NOT NULL,
    sensory_assessment_id VARCHAR(40) NOT NULL,
    descriptor_id VARCHAR(40) NOT NULL,
    intensity DOUBLE NULL,
    scale VARCHAR(20) NOT NULL,
    notes VARCHAR(300) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_assess_desc_assessment FOREIGN KEY (sensory_assessment_id)
        REFERENCES sensory_assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assess_desc_descriptor FOREIGN KEY (descriptor_id)
        REFERENCES sensory_descriptors (id) ON DELETE CASCADE
);

CREATE INDEX idx_assess_desc_assessment ON assessment_descriptors (sensory_assessment_id);

CREATE TABLE IF NOT EXISTS preference_records (
    id VARCHAR(40) NOT NULL,
    sensory_assessment_id VARCHAR(40) NOT NULL,
    user_id VARCHAR(40) NOT NULL,
    overall_liking DOUBLE NULL,
    liking_scale VARCHAR(20) NOT NULL,
    would_repeat VARCHAR(20) NULL,
    affective_context VARCHAR(200) NULL,
    affective_notes VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_preference_assessment UNIQUE (sensory_assessment_id),
    CONSTRAINT fk_preference_assessment FOREIGN KEY (sensory_assessment_id)
        REFERENCES sensory_assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_preference_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);
