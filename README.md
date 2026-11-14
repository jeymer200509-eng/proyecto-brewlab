# BrewLab — Laboratorio Digital de Café de Especialidad (V1.0)

> **Aviso de marca:** *BrewLab* se utiliza exclusivamente como nombre de trabajo interno para este proyecto técnico. No se asume su disponibilidad legal ni registral.

**BrewLab** es una estación de trabajo web para café de especialidad que conecta en un único ciclo auditable:
1. **Coffee Library (`/app/cafes`)**: Trazabilidad de lotes de café verde (`CoffeeLot`) y múltiples eventos de tostión (`RoastBatch`) con cálculo dinámico de edad en días calendario.
2. **Equipment & Water (`/app/equipos`)**: Inventario de equipos (molinos, drippers, balanzas, hervidores, refractómetros) y perfiles de agua (`WaterProfile`) donde los datos no medidos permanecen como `null` (`—`), nunca como `0`.
3. **Recipe Builder (`/app/recetas`)**: Diseño de recetas con cálculo determinista de ratio (`1:x = agua_g / dosis_g`, p. ej. `250 g / 15 g = 16,666…` → `1:16,7`), pasos ordenados (`RecipeStep`), reordenamiento, duplicación y validación no bloqueante de coherencia.
4. **Brew Assistant (`/app/preparacion`)**: Asistente paso a paso con cronómetro resiliente basado en marcas de tiempo reales (`startedAt`, `pausedAt`, `accumulatedPauseMs`), recuperación tras recarga de pestaña, exclusión del tiempo en pausa, protección idempotente contra doble clic (`idempotencyKey`), instantánea inmutable de la receta y separación entre parámetros previstos y medidos reales.
5. **Sensory Lab & Flavor Explorer (`/app/catacion`)**: Evaluación descriptiva interna de laboratorio (explícitamente diferenciada de un formulario CVA oficial), rueda concéntrica SVG original y lista jerárquica accesible por teclado (`Familia → Subfamilia → Descriptor`, versión `brewlab-v1.0`), intensidad y escala separadas por descriptor, descriptores personalizados y bloque afectivo separado (`PreferenceRecord`).
6. **Analytics & Comparison (`/app/analisis`)**: Historial filtrable multicriterio y comparador lado a lado para 2 o más sesiones con tamaño de muestra `N`, unidades explícitas, gráfico SVG con su tabla fuente y advertencias metodológicas cuando cambian múltiples variables simultáneamente (sin afirmar causalidad a partir de correlación).
7. **Experiment Lab (`/app/experimentos`)**: Diseño de experimentos (`Experiment` + `ExperimentTrial` vinculados a `BrewSession`) aislando una variable principal modificada frente a variables controladas, con sugerencias de hipótesis generadas por un motor determinista y explicable basado en reglas (`RuleRecommendationEngine`).
8. **Backup, Restauración y Semilla `[DEMO]` (`/app/ajustes`, `/app/respaldo`)**: Exportación e importación JSON con `schemaVersion`, prevalidación sin alterar datos existentes, vista previa y confirmación explícita; exportación CSV RFC 4180 y carga opcional de datos marcados explícitamente como `[DEMO]`.

---

## 1. Decisiones de Arquitectura y Seguridad

- **Arquitectura MVC renderizada en servidor (Sin microservicios ni API REST):** Servlets Java (`javax.servlet`, Servlet 3.1) + vistas JSP/JSTL (`POST-Redirect-GET`) con mejora progresiva en el navegador para el cronómetro y la rueda sensorial.
- **Autenticación con JWT + Protección CSRF:**
  - Al iniciar sesión o registrarse (`/auth/login`, `/auth/registro`), `JwtService` emite un token JWT firmado con `HMAC-SHA256` (`HS256`) almacenado en la cookie `brewlab_jwt` (`HttpOnly; SameSite=Strict`).
  - `JwtAuthFilter` verifica firma, expiración y `tokenVersion` del usuario en cada petición a `/app/*`, y exige validación CSRF por doble envío (`brewlab_csrf` cookie + campo `_csrf`) en todas las peticiones `POST`.
  - Contraseñas almacenadas con `PBKDF2WithHmacSHA256` (`PasswordHasher`, 120 000 iteraciones + sal aleatoria).
- **Persistencia Relacional SQL (Sin MongoDB):**
  - JDBC + pool de conexiones `HikariCP` + migraciones versionadas con verificación de suma `SHA-256` (`SchemaMigrator`, `V001` a `V006`).
  - Configuración primaria para **MySQL 9.5** (`jdbc:mysql://localhost:3306/brewlab`) con respaldo relacional automático en disco (`WEB-INF/data/brewlab_db` en modo compatible MySQL) cuando el esquema externo aún no ha sido aprovisionado por el administrador local.
- **Diseño de Interfaz (`apple-design`):**
  - Superficies translúcidas (`backdrop-filter`), tipografía con numerales tabulares (`font-variant-numeric: tabular-nums`) para cronómetro y tablas comparativas, respuesta inmediata en `pointerdown` con histéresis de `10px`, y soporte para `@media (prefers-reduced-motion: reduce)`, `(prefers-reduced-transparency: reduce)` y `(prefers-contrast: more)`.

---

## 2. Requisitos e Instalación

### Requisitos
- **Java JDK:** JDK 8 o superior (probado con JDK 25 compilando con `--release 8` para máxima compatibilidad con Apache Tomcat 8.5).
- **Servidor de aplicaciones:** Apache Tomcat 8.5.x (incluido en `C:\xampp\tomcat`).
- **Base de datos relacional:** MySQL Server 9.5 (opcionalmente aprovisionado con `db/setup.sql`; si no está activo, la aplicación utiliza automáticamente la base relacional SQL local en `WEB-INF/data/brewlab_db`).

### Configuración inicial
1. Generar secretos criptográficos locales (`brewlab.properties` y `db/setup.local.sql`):
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\setup-local.ps1
   ```
2. *(Opcional)* Si deseas usar el servidor MySQL 9.5 externo en lugar de la base SQL embebida en disco, ejecuta como administrador de MySQL:
   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe" -u root -p < .\db\setup.local.sql
   ```

---

## 3. Comandos de Compilación y Pruebas

El repositorio incluye **Maven Wrapper** (`mvnw.cmd`), por lo que no requiere instalar Maven globalmente:

```powershell
# Ejecutar toda la batería de pruebas unitarias y de integración (JUnit 5)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
.\mvnw.cmd test

# Compilar clases y empaquetar el artefacto WAR dejando las librerías listas en WEB-INF/lib
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
.\mvnw.cmd test package
```

Una vez iniciado Tomcat en XAMPP (`C:\xampp\tomcat\bin\startup.bat`), la aplicación queda disponible en:
- `http://localhost:8080/Proyecto%20BrewLab/`

---

## 4. Estructura del Proyecto

```text
Proyecto BrewLab/
├── pom.xml                                  # Configuración Maven (WAR in-place + JUnit 5)
├── mvnw / mvnw.cmd                          # Maven Wrapper reproducible
├── index.jsp                                # Redirección inicial hacia /app/inicio
├── manifest.webmanifest                     # Manifiesto de aplicación web instalable
├── assets/
│   ├── css/brewlab.css                      # Sistema de diseño Apple-style + tokens semánticos
│   └── js/brewlab.js                        # Feedback táctil en pointerdown y anti-doble envío
├── db/
│   ├── setup.template.sql                   # Plantilla SQL para MySQL 9.5
│   └── setup.sql                            # Script de aprovisionamiento MySQL
├── scripts/
│   └── setup-local.ps1                      # Generador de secretos JWT/DB locales
├── src/main/
│   ├── java/com/brewlab/
│   │   ├── app/                             # AppConfig, AppContainer, AppBootstrapListener
│   │   ├── security/                        # JwtService, PasswordHasher, CsrfTokens, Filtros
│   │   ├── domain/
│   │   │   ├── calc/                        # BrewRatio, RoastAge, BrewTimerEngine
│   │   │   ├── format/                      # Formats (null -> "—", nunca 0)
│   │   │   ├── model/                       # Entidades de dominio inmutables
│   │   │   ├── validation/                  # CoffeeValidators, RecipeValidators, FieldErrors
│   │   │   ├── sensory/                     # OriginalFlavorTaxonomy (brewlab-v1.0 + rueda SVG)
│   │   │   └── analytics/                   # SessionComparison, RuleRecommendationEngine
│   │   ├── data/
│   │   │   ├── db/                          # DataSourceFactory, SchemaMigrator, JdbcUtils
│   │   │   ├── repositories/                # Repositorios JDBC transaccionales
│   │   │   └── backup/                      # BackupService (JSON/CSV) y DemoDataSeeder
│   │   ├── features/                        # Servlets MVC (auth, dashboard, coffee, equipment,
│   │   │                                    # recipes, brew, sensory, analytics, experiments,
│   │   │                                    # settings, backup)
│   │   └── web/                             # WebSupport (flash messages, renderizado JSP)
│   └── resources/db/migration/
│       ├── V001__base_auth_and_settings.sql
│       ├── V002__coffee_library.sql
│       ├── V003__equipment_water_and_recipes.sql
│       ├── V004__brew_assistant.sql
│       ├── V005__sensory_lab.sql
│       └── V006__experiments.sql
├── src/test/java/com/brewlab/               # Pruebas automatizadas JUnit 5 (12 tests)
└── WEB-INF/
    ├── web.xml                              # Descriptor Servlet 3.1 (filtros y rutas MVC)
    ├── config/brewlab.properties            # Configuración local (ignorada en Git)
    └── views/                               # Vistas JSP/JSTL organizadas por módulo
```

---

## 5. Modelo de Datos Relacional (Esquema V6)

| Migración | Tablas | Propósito principal |
| :--- | :--- | :--- |
| **V001** | `users`, `user_settings` | Identidad autenticada por JWT (`token_version`) y preferencias separadas del dominio del café. |
| **V002** | `coffee_lots`, `roast_batches` | Separación `1:N` entre lote de café verde (`CoffeeLot`) y eventos de tostión (`RoastBatch`). |
| **V003** | `equipment`, `water_profiles`, `recipes`, `recipe_equipment`, `recipe_steps` | Equipos, agua, recetas y pasos ordenados con dosis de agua individual y acumulada. |
| **V004** | `brew_sessions`, `brew_session_equipment`, `brew_events` | Sesiones con instantánea JSON de la receta, estado del cronómetro por timestamps y eventos idempotentes. |
| **V005** | `sensory_descriptors`, `sensory_assessments`, `assessment_descriptors`, `preference_records` | Taxonomía propia versionada (`brewlab-v1.0`), evaluación descriptiva, intensidad por descriptor y bloque afectivo separado. |
| **V006** | `experiments`, `experiment_trials` | Experimentos controlados y ensayos vinculados a `brew_sessions`. |

---

## 6. Pruebas Automatizadas Ejecutadas y Verificadas

Todas las pruebas se ejecutan con `.\mvnw.cmd test` sobre una base relacional limpia migrada de `V001` a `V006`:

1. `DomainCalcTest` (4 tests):
   - Cálculo exacto de ratio `250 g / 15 g = 16,666…` (`1:16,7`) y protección ante división por cero o `null`.
   - Cálculo dinámico de edad desde tostión en días calendario y detección de fechas futuras.
   - Formato de valores donde `null` produce `—` y nunca `0`.
2. `SecurityAndRepositoryTest` (1 test):
   - Hash `PBKDF2WithHmacSHA256`, emisión/verificación/expiración de JWT `HS256`, invalidación por `tokenVersion`, validación CSRF por doble envío y persistencia de `UserSettings`.
3. `CoffeeLibraryTest` (1 test):
   - CRUD de `CoffeeLot` y múltiples `RoastBatch`es con edades distintas, búsqueda, archivado y preservación de `null` en humedad/densidad/Agtron.
4. `RecipeAndEquipmentTest` (1 test):
   - CRUD de `Equipment`, `WaterProfile` (`null` nunca convertido en `0`), `Recipe` + `RecipeStep`s, reordenamiento, duplicación y advertencias de coherencia de agua/tiempos.
5. `BrewAssistantTimerTest` (2 tests):
   - Inicio, pausa (la pausa no suma tiempo), reanudación y reconstrucción exacta del tiempo tras recarga de pestaña a partir de `startedAt`, `pausedAt` y `accumulatedPauseMs`.
   - Registro tardío de etapas, protección idempotente contra doble clic (`idempotencyKey`), preservación de la instantánea de la receta tras editar la receta original y corrección auditable posterior.
6. `SensoryLabTest` (1 test):
   - Siembra idempotente de la taxonomía original `brewlab-v1.0`, sectores SVG de la rueda concéntrica, descriptores personalizados, almacenamiento separado de `descriptorId` + `intensity` + `scale` y separación estricta de `PreferenceRecord`.
7. `AnalyticsAndExperimentTest` (1 test):
   - Comparador `SessionComparison` (detección de 1 variable cambiada vs. advertencia metodológica de múltiples variables simultáneas), motor de reglas `RuleRecommendationEngine` (`R-01` a `R-05`) y flujo completo de `Experiment` + `ExperimentTrial`.
8. `BackupAndFullCycleTest` (1 test):
   - Recorrido vertical completo (CA-01 a CA-12), exportación JSON con `schemaVersion`, rechazo de JSON corrupto o con versión incompatible sin alterar los datos actuales, exportación CSV con celdas `null` vacías y restauración íntegra.
