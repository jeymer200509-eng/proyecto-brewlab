package com.brewlab.data;

import com.brewlab.data.backup.BackupService;
import com.brewlab.data.backup.DemoDataSeeder;
import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.BrewSessionRepository;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.EquipmentRepository;
import com.brewlab.data.repositories.ExperimentRepository;
import com.brewlab.data.repositories.RecipeRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.SensoryRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.data.repositories.WaterProfileRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.analytics.SessionComparison;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.WaterProfile;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba integral de extremo a extremo del ciclo vertical MVP de BrewLab (CA-01 a CA-12):
 * CoffeeLot -> 2 RoastBatches -> Equipment & WaterProfile -> Recipe (1:16,7) -> BrewSession ->
 * SensoryAssessment + PreferenceRecord -> Comparador & Experiment Lab -> Backup JSON/CSV con
 * prevalidación de esquema y restauración íntegra.
 */
class BackupAndFullCycleTest {

    @Test
    void fullEndToEndVerticalCycleAndJsonCsvBackupRestoreWithSchemaValidation() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            DataSource ds = mds.getDataSource();
            SchemaMigrator migrator = new SchemaMigrator(ds);
            migrator.migrate();
            int currentSchema = migrator.currentVersion();
            assertEquals(6, currentSchema);

            Instant now = Instant.parse("2026-04-14T10:00:00Z");
            LocalDate today = LocalDate.of(2026, 4, 14);
            String userId = Ids.newId();
            new UserRepository(ds).create(userId, "fullcycle@brewlab.local", "Sofía", "hash", now);

            CoffeeLotRepository lotRepo = new CoffeeLotRepository(ds);
            RoastBatchRepository batchRepo = new RoastBatchRepository(ds);
            EquipmentRepository eqRepo = new EquipmentRepository(ds);
            WaterProfileRepository wpRepo = new WaterProfileRepository(ds);
            RecipeRepository recipeRepo = new RecipeRepository(ds);
            BrewSessionRepository sessionRepo = new BrewSessionRepository(ds);
            SensoryRepository sensoryRepo = new SensoryRepository(ds);
            ExperimentRepository expRepo = new ExperimentRepository(ds);

            DemoDataSeeder seeder = new DemoDataSeeder(
                    lotRepo, batchRepo, eqRepo, wpRepo, recipeRepo, sessionRepo, sensoryRepo, expRepo
            );
            BackupService backupService = new BackupService(
                    ds, lotRepo, batchRepo, eqRepo, wpRepo, recipeRepo, sessionRepo, sensoryRepo, expRepo
            );

            // 1. Sembrar recorrido vertical completo [DEMO]
            seeder.seed(userId, today, now);

            assertEquals(1, lotRepo.findAll(userId, null, false).size());
            List<RoastBatch> batches = batchRepo.findAllByUser(userId, false);
            assertEquals(2, batches.size());
            assertEquals(2, eqRepo.findAllByUser(userId).size());
            assertEquals(1, wpRepo.findAllByUser(userId).size());
            assertEquals(1, recipeRepo.findAllByUser(userId).size());
            assertEquals("1:16,7", recipeRepo.findAllByUser(userId).get(0).getRatioDisplay());

            List<BrewSession> sessions = sessionRepo.findAllByUser(userId);
            assertEquals(2, sessions.size());
            List<SensoryAssessment> assessments = sensoryRepo.findAllByUser(userId);
            assertEquals(2, assessments.size());
            assertEquals(1, expRepo.findAllByUser(userId).size());

            // 2. Comparar las 2 sesiones del ciclo
            Map<String, SensoryAssessment> assessBySession = new HashMap<>();
            for (SensoryAssessment a : assessments) {
                assessBySession.put(a.getBrewSessionId(), a);
            }
            SessionComparison comp = SessionComparison.build(sessions, assessBySession);
            assertEquals(2, comp.getSampleSize());
            assertEquals(1, comp.getChangedVariables().size());

            // 3. Exportar JSON y verificar prevalidación
            String exportedJson = backupService.exportJson(userId, currentSchema, now);
            assertNotNull(exportedJson);
            assertTrue(exportedJson.contains("\"format\": \"brewlab-backup\""));

            BackupService.ImportPreview validPreview = backupService.prevalidate(exportedJson, currentSchema);
            assertTrue(validPreview.isValid());
            assertEquals(1, validPreview.getCoffeeLotsCount());
            assertEquals(2, validPreview.getRoastBatchesCount());
            assertEquals(2, validPreview.getSessionsCount());
            assertEquals(2, validPreview.getAssessmentsCount());
            assertEquals(1, validPreview.getExperimentsCount());

            // 4. Rechazar JSON inválido o con schemaVersion desconocido SIN alterar los datos existentes
            String futureVersionJson = exportedJson.replace("\"schemaVersion\": 6", "\"schemaVersion\": 999");
            BackupService.ImportPreview badVersionPreview = backupService.prevalidate(futureVersionJson, currentSchema);
            assertFalse(badVersionPreview.isValid());
            assertThrows(IllegalArgumentException.class,
                    () -> backupService.restoreJson(userId, futureVersionJson, currentSchema));
            assertThrows(IllegalArgumentException.class,
                    () -> backupService.restoreJson(userId, "{invalid json", currentSchema));

            // Verificar que los datos actuales del usuario siguen intactos tras intentos fallidos
            assertEquals(1, lotRepo.findAll(userId, null, false).size());
            assertEquals(2, sessionRepo.findAllByUser(userId).size());

            // 5. Exportar CSV y verificar que null queda como celda vacía (nunca 0)
            String roastCsv = backupService.exportCsv(userId, "roast_batches");
            assertTrue(roastCsv.contains("DEMO-RST-CLARA"));
            // En DEMO-RST-CLARA agtron es null -> debe verse "12.9,,250.0" (vacío entre comas, no 0)
            assertTrue(roastCsv.contains("12.9,,250.0"));

            // 6. Limpiar datos y restaurar desde el respaldo JSON válido
            backupService.clearUserData(userId);
            assertEquals(0, lotRepo.findAll(userId, null, false).size());
            assertEquals(0, sessionRepo.findAllByUser(userId).size());

            BackupService.ImportPreview restored = backupService.restoreJson(userId, exportedJson, currentSchema);
            assertTrue(restored.isValid());
            assertEquals(1, lotRepo.findAll(userId, null, false).size());
            assertEquals(2, batchRepo.findAllByUser(userId, false).size());
            assertEquals(2, sessionRepo.findAllByUser(userId).size());
            assertEquals(2, sensoryRepo.findAllByUser(userId).size());
            assertEquals(1, expRepo.findAllByUser(userId).size());

            // Verificar que los campos null siguen siendo null tras restaurar
            WaterProfile restoredWater = wpRepo.findAllByUser(userId).get(0);
            assertNull(restoredWater.getAlkalinityKh(), "Alcalinidad no medida debe seguir siendo null tras restaurar");
            assertNull(restoredWater.getNotes(), "Notas vacías deben seguir siendo null tras restaurar");
        }
    }
}
