package com.brewlab.data;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.BrewSessionRepository;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.RecipeRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.calc.BrewTimerEngine;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;
import com.brewlab.domain.model.RoastBatch;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias y de integración para el motor de cronómetro resiliente (BrewTimerEngine)
 * y el repositorio de sesiones de preparación (BrewSessionRepository).
 */
class BrewAssistantTimerTest {

    @Test
    void timerPauseAndResumeExcludesPauseDurationAndRecoversOnReopen() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            DataSource ds = mds.getDataSource();
            new SchemaMigrator(ds).migrate();

            UserRepository userRepo = new UserRepository(ds);
            BrewSessionRepository sessionRepo = new BrewSessionRepository(ds);

            Instant t0 = Instant.parse("2026-04-10T10:00:00Z");
            String userId = Ids.newId();
            userRepo.create(userId, "barista@brewlab.local", "Barista", "hash", t0);

            List<RecipeStep> steps = Arrays.asList(
                    new RecipeStep("st-1", "r-1", 1, "Bloom", "Humedecer cama", 0, 35, 45.0, 45.0, 5),
                    new RecipeStep("st-2", "r-1", 2, "Primer vertido", "Espiral central", 35, 45, 105.0, 150.0, 5),
                    new RecipeStep("st-3", "r-1", 3, "Segundo vertido", "Pulso final", 80, 60, 100.0, 250.0, 10)
            );
            String snapshotJson = BrewSession.serializeStepsSnapshot(steps);

            String sessionId = Ids.newId();
            BrewSession initial = new BrewSession(
                    sessionId, userId, null, null, null, null, Collections.<String>emptyList(),
                    BrewSession.STATUS_PREPARED, null, null, 0L, null,
                    "V60 Estándar 15g/250g", "V60", 15.0, 250.0, 93.0, "22 clics", 150, 180,
                    snapshotJson,
                    15.0, null, 93.0, "22 clics", null, null, null,
                    false, Collections.<BrewEvent>emptyList(), t0, t0
            );
            sessionRepo.create(initial);

            // 1. Iniciar en t0 (10:00:00Z)
            BrewSession active = sessionRepo.startSession(userId, sessionId, t0, "START-1");
            assertEquals(BrewSession.STATUS_ACTIVE, active.getStatus());
            assertEquals(0L, BrewTimerEngine.computeElapsedMs(active, t0));

            // 2. Avanzan 30 segundos (10:00:30Z) -> elapsed = 30_000 ms
            Instant t30 = t0.plusSeconds(30);
            assertEquals(30_000L, BrewTimerEngine.computeElapsedMs(active, t30));

            // 3. Pausar en t30 (10:00:30Z)
            BrewSession paused = sessionRepo.pauseSession(userId, sessionId, t30, "PAUSE-1");
            assertEquals(BrewSession.STATUS_PAUSED, paused.getStatus());
            assertEquals(30_000L, BrewTimerEngine.computeElapsedMs(paused, t30));

            // 4. Mientras está en pausa durante 45 segundos (10:01:15Z), la pausa NO suma tiempo
            Instant t75 = t0.plusSeconds(75);
            assertEquals(30_000L, BrewTimerEngine.computeElapsedMs(paused, t75));

            // 5. Reanudar en t75 (10:01:15Z) -> accumulatedPauseMs = 45_000 ms, elapsed sigue en 30_000 ms
            BrewSession resumed = sessionRepo.resumeSession(userId, sessionId, t75, "RESUME-1");
            assertEquals(BrewSession.STATUS_ACTIVE, resumed.getStatus());
            assertEquals(45_000L, resumed.getAccumulatedPauseMs());
            assertNull(resumed.getPausedAt());
            assertEquals(30_000L, BrewTimerEngine.computeElapsedMs(resumed, t75));

            // 6. Simular recarga de página / reapertura de pestaña 20 segundos después (10:01:35Z)
            // Elapsed esperado = (95s - 0s) - 45s de pausa = 50_000 ms
            Instant t95 = t0.plusSeconds(95);
            BrewSession reloaded = sessionRepo.findById(userId, sessionId);
            assertNotNull(reloaded);
            assertEquals(50_000L, BrewTimerEngine.computeElapsedMs(reloaded, t95));
        }
    }

    @Test
    void lateStepRegistrationAndIdempotencyAndImmutableRecipeSnapshot() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            DataSource ds = mds.getDataSource();
            new SchemaMigrator(ds).migrate();

            UserRepository userRepo = new UserRepository(ds);
            CoffeeLotRepository lotRepo = new CoffeeLotRepository(ds);
            RoastBatchRepository batchRepo = new RoastBatchRepository(ds);
            RecipeRepository recipeRepo = new RecipeRepository(ds);
            BrewSessionRepository sessionRepo = new BrewSessionRepository(ds);

            Instant t0 = Instant.parse("2026-04-10T12:00:00Z");
            String userId = Ids.newId();
            userRepo.create(userId, "lab@brewlab.local", "Lab", "hash", t0);

            String lotId = Ids.newId();
            CoffeeLot lot = new CoffeeLot(
                    lotId, userId, "LOT-GEISHA", "Geisha Volcán", "Panamá", "Chiriquí",
                    "Boquete", "Finca Hartmann", "Familia Hartmann", "Arabica", "Geisha",
                    "Natural", Integer.valueOf(1850), null, "2026", null,
                    null, null, null, "Lote Geisha", false, null, t0, t0
            );
            lotRepo.create(lot);

            String batchId = Ids.newId();
            RoastBatch batch = new RoastBatch(
                    batchId, userId, lotId, "RST-G1", LocalDate.of(2026, 4, 1),
                    "Clara", "Tostador Lab", null, null, null, null, null, null, null,
                    false, null, t0, t0
            );
            batchRepo.create(batch);

            String recipeId = Ids.newId();
            List<RecipeStep> originalSteps = Arrays.asList(
                    new RecipeStep(Ids.newId(), recipeId, 1, "Bloom", "Verter 45 g", 0, 35, 45.0, 45.0, 5),
                    new RecipeStep(Ids.newId(), recipeId, 2, "Vertido único", "Completar hasta 250 g", 35, 110, 205.0, 250.0, 10)
            );
            Recipe recipe = new Recipe(
                    recipeId, userId, "V60 Geisha 1:16,7", "V60", lotId, batchId,
                    Double.valueOf(15.0), Double.valueOf(250.0), Double.valueOf(93.0),
                    "20 clics", Integer.valueOf(145), Integer.valueOf(165),
                    Collections.<String>emptyList(), null, "Receta original", 1, false,
                    originalSteps, t0, t0
            );
            recipeRepo.create(recipe);

            String sessionId = Ids.newId();
            BrewSession session = new BrewSession(
                    sessionId, userId, recipe.getId(), lot.getId(), batch.getId(), null,
                    Collections.<String>emptyList(), BrewSession.STATUS_ACTIVE, t0, null, 0L, null,
                    recipe.getName(), recipe.getMethod(), recipe.getDoseG(), recipe.getWaterG(),
                    recipe.getTemperatureC(), recipe.getGrindSetting(),
                    recipe.getTargetTimeMinS(), recipe.getTargetTimeMaxS(),
                    BrewSession.serializeStepsSnapshot(recipe.getSteps()),
                    15.0, null, 93.0, "20 clics", null, null, null,
                    false, Collections.<BrewEvent>emptyList(), t0, t0
            );
            sessionRepo.create(session);

            // Registro tardío del paso 1: el objetivo era terminar a los 35s, pero se registra a los 42s con 47.5g reales
            Instant t42 = t0.plusSeconds(42);
            RecipeStep step1 = BrewTimerEngine.findCurrentStep(session);
            assertNotNull(step1);
            assertEquals(1, step1.getOrder());

            sessionRepo.recordStepEvent(
                    userId, sessionId, step1, false, 47.5, "Registrado 7s tarde", t42, "IDEM-STEP-1"
            );
            // Simular doble clic con la misma clave de idempotencia o sobre el mismo paso
            BrewSession afterDuplicateClick = sessionRepo.recordStepEvent(
                    userId, sessionId, step1, false, 47.5, "Doble clic", t42.plusSeconds(1), "IDEM-STEP-1"
            );

            assertEquals(1, afterDuplicateClick.getEvents().size());
            BrewEvent ev1 = afterDuplicateClick.getEvents().get(0);
            assertEquals(42_000L, ev1.getElapsedMs());
            assertEquals(45.0, ev1.getTargetValue(), 0.001);
            assertEquals(47.5, ev1.getActualValue(), 0.001);

            // Siguiente paso activo debe ser el paso 2
            RecipeStep currentNow = BrewTimerEngine.findCurrentStep(afterDuplicateClick);
            assertNotNull(currentNow);
            assertEquals(2, currentNow.getOrder());

            // Modificar la receta original posteriormente (cambiar dosis a 20g y agua a 320g)
            Recipe modifiedRecipe = new Recipe(
                    recipeId, userId, "V60 Modificada", "V60", lotId, batchId,
                    Double.valueOf(20.0), Double.valueOf(320.0), Double.valueOf(90.0),
                    "28 clics", Integer.valueOf(180), Integer.valueOf(210),
                    Collections.<String>emptyList(), null, "Cambiada", 2, false,
                    Collections.singletonList(new RecipeStep(Ids.newId(), recipeId, 1, "Único", "Todo", 0, 180, 320.0, 320.0, null)),
                    t0, t0.plusSeconds(100)
            );
            recipeRepo.updateHeaderAndEquipment(modifiedRecipe);

            // Finalizar la sesión a los 158s con 251.0 g reales y bebida final no medida (null)
            Instant t158 = t0.plusSeconds(158);
            BrewSession completed = sessionRepo.completeSession(
                    userId, sessionId, 15.1, 251.0, 92.5, "20 clics",
                    null, null, "Extracción limpia y floral", t158, "IDEM-COMPLETE"
            );

            // Verificar que la instantánea histórica de la sesión NO cambió pese a editar la receta
            assertEquals("V60 Geisha 1:16,7", completed.getPlannedRecipeName());
            assertEquals(15.0, completed.getPlannedDoseG(), 0.001);
            assertEquals(250.0, completed.getPlannedWaterG(), 0.001);
            assertEquals(2, completed.getSnapshotSteps().size());

            // Verificar separación de previstos vs medidos y preservación de null (bebida final no medida)
            assertEquals(15.1, completed.getActualDoseG(), 0.001);
            assertEquals(251.0, completed.getActualWaterG(), 0.001);
            assertEquals(Integer.valueOf(158), completed.getTotalTimeSeconds());
            assertNull(completed.getFinalBeverageWeightG());

            // Corrección auditable posterior
            BrewSession corrected = sessionRepo.recordAuditableCorrection(
                    userId, sessionId, 15.1, 251.0, 92.5, "20 clics",
                    Integer.valueOf(158), 218.5, "Se pesó la bebida final en balanza auxiliar",
                    t158.plusSeconds(60), "IDEM-CORR-1"
            );
            assertEquals(218.5, corrected.getFinalBeverageWeightG(), 0.001);
            assertTrue(corrected.getEvents().stream()
                    .anyMatch(e -> BrewEvent.TYPE_CORRECTION.equals(e.getEventType())));
        }
    }
}
