-- V001: Usuarios (autenticación JWT) y preferencias de usuario

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) NOT NULL,
  email VARCHAR(190) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  token_version INT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_settings (
  user_id VARCHAR(36) NOT NULL,
  sound_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  keep_screen_awake BOOLEAN NOT NULL DEFAULT FALSE,
  timer_theme VARCHAR(20) NOT NULL DEFAULT 'auto',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
