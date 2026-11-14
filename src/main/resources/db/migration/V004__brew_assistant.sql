-- V004: Asistente de Preparación (BrewSession y BrewEvent)

CREATE TABLE IF NOT EXISTS brew_sessions (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  recipe_id VARCHAR(36) NULL,
  coffee_lot_id VARCHAR(36) NULL,
  roast_batch_id VARCHAR(36) NULL,
  water_profile_id VARCHAR(36) NULL,
  status VARCHAR(24) NOT NULL,
  started_at TIMESTAMP NULL,
  paused_at TIMESTAMP NULL,
  accumulated_pause_ms BIGINT NOT NULL DEFAULT 0,
  completed_at TIMESTAMP NULL,
  planned_recipe_name VARCHAR(180) NOT NULL,
  planned_method VARCHAR(80) NOT NULL,
  planned_dose_g DOUBLE NULL,
  planned_water_g DOUBLE NULL,
  planned_temperature_c DOUBLE NULL,
  planned_grind_setting VARCHAR(120) NULL,
  planned_target_time_min_s INT NULL,
  planned_target_time_max_s INT NULL,
  recipe_snapshot_json TEXT NOT NULL,
  actual_dose_g DOUBLE NULL,
  actual_water_g DOUBLE NULL,
  actual_temperature_c DOUBLE NULL,
  actual_grind_setting VARCHAR(120) NULL,
  total_time_seconds INT NULL,
  final_beverage_weight_g DOUBLE NULL,
  notes TEXT NULL,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_bs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_bs_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE SET NULL,
  CONSTRAINT fk_bs_lot FOREIGN KEY (coffee_lot_id) REFERENCES coffee_lots (id) ON DELETE SET NULL,
  CONSTRAINT fk_bs_batch FOREIGN KEY (roast_batch_id) REFERENCES roast_batches (id) ON DELETE SET NULL,
  CONSTRAINT fk_bs_water FOREIGN KEY (water_profile_id) REFERENCES water_profiles (id) ON DELETE SET NULL
);

CREATE INDEX idx_brew_sessions_user ON brew_sessions (user_id, created_at);

CREATE TABLE IF NOT EXISTS brew_session_equipment (
  brew_session_id VARCHAR(36) NOT NULL,
  equipment_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (brew_session_id, equipment_id),
  CONSTRAINT fk_bse_session FOREIGN KEY (brew_session_id) REFERENCES brew_sessions (id) ON DELETE CASCADE,
  CONSTRAINT fk_bse_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS brew_events (
  id VARCHAR(36) NOT NULL,
  brew_session_id VARCHAR(36) NOT NULL,
  recipe_step_id VARCHAR(36) NULL,
  step_order INT NULL,
  event_type VARCHAR(40) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  elapsed_ms BIGINT NOT NULL,
  target_value DOUBLE NULL,
  actual_value DOUBLE NULL,
  idempotency_key VARCHAR(96) NOT NULL,
  notes TEXT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_brew_events_idem UNIQUE (brew_session_id, idempotency_key),
  CONSTRAINT fk_brew_events_session FOREIGN KEY (brew_session_id) REFERENCES brew_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_brew_events_session ON brew_events (brew_session_id, occurred_at);
