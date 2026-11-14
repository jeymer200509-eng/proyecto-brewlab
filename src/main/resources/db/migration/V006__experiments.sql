-- =====================================================================
-- V006__experiments.sql
-- BrewLab Fase 6: Laboratorio de Experimentación (Experiment y ExperimentTrial)
-- =====================================================================

CREATE TABLE IF NOT EXISTS experiments (
    id VARCHAR(40) NOT NULL,
    user_id VARCHAR(40) NOT NULL,
    coffee_lot_id VARCHAR(40) NULL,
    roast_batch_id VARCHAR(40) NULL,
    title VARCHAR(180) NOT NULL,
    question VARCHAR(500) NOT NULL,
    objective VARCHAR(500) NOT NULL,
    hypothesis VARCHAR(800) NOT NULL,
    modified_variable VARCHAR(120) NOT NULL,
    controlled_variables VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    user_conclusion VARCHAR(1500) NULL,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_experiments_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_experiments_lot FOREIGN KEY (coffee_lot_id)
        REFERENCES coffee_lots (id) ON DELETE SET NULL,
    CONSTRAINT fk_experiments_batch FOREIGN KEY (roast_batch_id)
        REFERENCES roast_batches (id) ON DELETE SET NULL
);

CREATE INDEX idx_experiments_user ON experiments (user_id, created_at);

CREATE TABLE IF NOT EXISTS experiment_trials (
    id VARCHAR(40) NOT NULL,
    experiment_id VARCHAR(40) NOT NULL,
    brew_session_id VARCHAR(40) NOT NULL,
    trial_order INT NOT NULL,
    label VARCHAR(120) NOT NULL,
    modified_value VARCHAR(160) NULL,
    notes VARCHAR(600) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_exp_trials_experiment FOREIGN KEY (experiment_id)
        REFERENCES experiments (id) ON DELETE CASCADE,
    CONSTRAINT fk_exp_trials_session FOREIGN KEY (brew_session_id)
        REFERENCES brew_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_exp_trials_experiment ON experiment_trials (experiment_id, trial_order);
