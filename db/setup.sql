-- =============================================================================
-- BrewLab — Script de aprovisionamiento de MySQL 8.0+ / 9.x
-- =============================================================================
-- Crea las bases de datos relacionales 'brewlab' y 'brewlab_test' y el usuario
-- dedicado 'brewlab'@'localhost'.
--
-- Nota: Si ejecutaste scripts/setup-local.ps1, usa preferiblemente
-- db/setup.local.sql (excluido de Git), que ya incluye la contraseña aleatoria
-- sincronizada con WEB-INF/config/brewlab.properties.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS brewlab
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS brewlab_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'brewlab'@'localhost' IDENTIFIED BY 'CambiarEnSetupLocal_2026!';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON brewlab.* TO 'brewlab'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON brewlab_test.* TO 'brewlab'@'localhost';

FLUSH PRIVILEGES;
