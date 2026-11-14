-- V003: Equipos (Equipment), Perfiles de Agua (WaterProfile), Recetas (Recipe) y Pasos (RecipeStep)

CREATE TABLE IF NOT EXISTS equipment (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  type VARCHAR(40) NOT NULL,
  brand VARCHAR(100) NULL,
  model VARCHAR(120) NULL,
  name VARCHAR(160) NOT NULL,
  settings_scale VARCHAR(140) NULL,
  notes TEXT NULL,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_equipment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_equipment_user ON equipment (user_id);

CREATE TABLE IF NOT EXISTS water_profiles (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  name VARCHAR(160) NOT NULL,
  source VARCHAR(140) NULL,
  tds_ppm DOUBLE NULL,
  hardness_gh DOUBLE NULL,
  alkalinity_kh DOUBLE NULL,
  minerals VARCHAR(240) NULL,
  measured_at DATE NULL,
  notes TEXT NULL,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_water_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_water_profiles_user ON water_profiles (user_id);

CREATE TABLE IF NOT EXISTS recipes (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  name VARCHAR(180) NOT NULL,
  method VARCHAR(80) NOT NULL,
  coffee_lot_id VARCHAR(36) NULL,
  roast_batch_id VARCHAR(36) NULL,
  dose_g DOUBLE NOT NULL,
  water_g DOUBLE NOT NULL,
  temperature_c DOUBLE NULL,
  grind_setting VARCHAR(120) NULL,
  target_time_min_s INT NULL,
  target_time_max_s INT NULL,
  water_profile_id VARCHAR(36) NULL,
  notes TEXT NULL,
  version INT NOT NULL DEFAULT 1,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_recipes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_recipes_lot FOREIGN KEY (coffee_lot_id) REFERENCES coffee_lots (id) ON DELETE SET NULL,
  CONSTRAINT fk_recipes_batch FOREIGN KEY (roast_batch_id) REFERENCES roast_batches (id) ON DELETE SET NULL,
  CONSTRAINT fk_recipes_water FOREIGN KEY (water_profile_id) REFERENCES water_profiles (id) ON DELETE SET NULL
);

CREATE INDEX idx_recipes_user ON recipes (user_id);

CREATE TABLE IF NOT EXISTS recipe_equipment (
  recipe_id VARCHAR(36) NOT NULL,
  equipment_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (recipe_id, equipment_id),
  CONSTRAINT fk_re_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
  CONSTRAINT fk_re_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS recipe_steps (
  id VARCHAR(36) NOT NULL,
  recipe_id VARCHAR(36) NOT NULL,
  step_order INT NOT NULL,
  title VARCHAR(140) NOT NULL,
  instruction TEXT NOT NULL,
  start_seconds INT NOT NULL,
  duration_seconds INT NULL,
  water_dose_g DOUBLE NULL,
  cumulative_water_g DOUBLE NULL,
  tolerance_seconds INT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_recipe_steps_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
);

CREATE INDEX idx_recipe_steps_recipe ON recipe_steps (recipe_id, step_order);
