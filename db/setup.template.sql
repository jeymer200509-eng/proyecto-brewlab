-- BrewLab — creación de bases de datos y usuario dedicado (MySQL 8+/9.x)
-- NO ejecutar esta plantilla directamente: scripts/setup-local.ps1 genera
-- db/setup.local.sql (excluido de Git) reemplazando __BREWLAB_DB_PASSWORD__.
-- Ejecutar una sola vez con una cuenta administradora (p. ej. root):
--   "C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe" -u root -p < db\setup.local.sql
-- Las TABLAS no se crean aquí: la aplicación aplica sus migraciones versionadas al arrancar.

CREATE DATABASE IF NOT EXISTS brewlab
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS brewlab_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'brewlab'@'localhost' IDENTIFIED BY '__BREWLAB_DB_PASSWORD__';
ALTER USER 'brewlab'@'localhost' IDENTIFIED BY '__BREWLAB_DB_PASSWORD__';

-- Privilegios limitados a las dos bases de BrewLab (DDL necesario para las migraciones).
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON brewlab.* TO 'brewlab'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
  ON brewlab_test.* TO 'brewlab'@'localhost';
FLUSH PRIVILEGES;
