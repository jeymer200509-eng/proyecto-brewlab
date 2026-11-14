-- V002: Biblioteca de Cafés (CoffeeLot) y Lotes de Tostión (RoastBatch)

CREATE TABLE IF NOT EXISTS coffee_lots (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(180) NOT NULL,
  country VARCHAR(100) NULL,
  region VARCHAR(100) NULL,
  municipality VARCHAR(100) NULL,
  farm VARCHAR(140) NULL,
  producer VARCHAR(140) NULL,
  species VARCHAR(80) NULL,
  variety VARCHAR(140) NULL,
  process VARCHAR(120) NULL,
  altitude_min INT NULL,
  altitude_max INT NULL,
  harvest VARCHAR(40) NULL,
  received_on DATE NULL,
  moisture_pct DOUBLE NULL,
  density DOUBLE NULL,
  screen_size VARCHAR(40) NULL,
  notes TEXT NULL,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  archived_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_coffee_lots_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_coffee_lots_user ON coffee_lots (user_id);

CREATE TABLE IF NOT EXISTS roast_batches (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  coffee_lot_id VARCHAR(36) NOT NULL,
  batch_code VARCHAR(64) NOT NULL,
  roast_date DATE NOT NULL,
  roast_level VARCHAR(60) NULL,
  roaster VARCHAR(140) NULL,
  machine VARCHAR(140) NULL,
  roast_notes TEXT NULL,
  weight_loss_pct DOUBLE NULL,
  agtron DOUBLE NULL,
  opened_on DATE NULL,
  preferred_by_date DATE NULL,
  available_weight_g DOUBLE NULL,
  is_demo BOOLEAN NOT NULL DEFAULT FALSE,
  archived_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_roast_batches_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_roast_batches_lot FOREIGN KEY (coffee_lot_id) REFERENCES coffee_lots (id) ON DELETE CASCADE
);

CREATE INDEX idx_roast_batches_lot ON roast_batches (coffee_lot_id);
CREATE INDEX idx_roast_batches_user ON roast_batches (user_id);
