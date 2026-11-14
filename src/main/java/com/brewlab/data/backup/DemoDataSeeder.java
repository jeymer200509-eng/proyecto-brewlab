package com.brewlab.data.backup;

import com.brewlab.data.repositories.BrewSessionRepository;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.EquipmentRepository;
import com.brewlab.data.repositories.ExperimentRepository;
import com.brewlab.data.repositories.RecipeRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.SensoryRepository;
import com.brewlab.data.repositories.WaterProfileRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.AssessmentDescriptor;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Experiment;
import com.brewlab.domain.model.ExperimentTrial;
import com.brewlab.domain.model.PreferenceRecord;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.WaterProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Semilla de datos de demostración marcada explícitamente con {@code [DEMO]} y {@code isDemo = true},
 * que recorre el ciclo vertical completo de BrewLab sin inventar métricas ocultas.
 */
public final class DemoDataSeeder {
    private final CoffeeLotRepository coffeeLotRepository;
    private final RoastBatchRepository roastBatchRepository;
    private final EquipmentRepository equipmentRepository;
    private final WaterProfileRepository waterProfileRepository;
    private final RecipeRepository recipeRepository;
    private final BrewSessionRepository brewSessionRepository;
    private final SensoryRepository sensoryRepository;
    private final ExperimentRepository experimentRepository;

    public DemoDataSeeder(CoffeeLotRepository coffeeLotRepository,
                          RoastBatchRepository roastBatchRepository,
                          EquipmentRepository equipmentRepository,
                          WaterProfileRepository waterProfileRepository,
                          RecipeRepository recipeRepository,
                          BrewSessionRepository brewSessionRepository,
                          SensoryRepository sensoryRepository,
                          ExperimentRepository experimentRepository) {
        this.coffeeLotRepository = coffeeLotRepository;
        this.roastBatchRepository = roastBatchRepository;
        this.equipmentRepository = equipmentRepository;
        this.waterProfileRepository = waterProfileRepository;
        this.recipeRepository = recipeRepository;
        this.brewSessionRepository = brewSessionRepository;
        this.sensoryRepository = sensoryRepository;
        this.experimentRepository = experimentRepository;
    }

    public void seed(String userId, LocalDate today, Instant now) {
        sensoryRepository.seedDefaultTaxonomyIfNeeded();

        // 1. CoffeeLot [DEMO]
        String lotId = Ids.newId();
        CoffeeLot lot = new CoffeeLot(
                lotId, userId, "DEMO-HUI-01", "[DEMO] Finca El Diviso — Pink Bourbon",
                "Colombia", "Huila", "Pitalito", "El Diviso", "Familia Lasso",
                "Coffea arabica", "Pink Bourbon", "Lavado con oxidación térmica",
                Integer.valueOf(1820), Integer.valueOf(1900), "2026-A", today.minusDays(14),
                Double.valueOf(10.8), Double.valueOf(712.0), "Malla 16-17",
                "Registro de demostración: lote floral y cítrico", true, null, now, now
        );
        coffeeLotRepository.create(lot);

        // 2. Dos RoastBatches [DEMO] con edades distintas
        String batch1Id = Ids.newId();
        RoastBatch batch1 = new RoastBatch(
                batch1Id, userId, lotId, "DEMO-RST-CLARA",
                today.minusDays(9), "Clara", "Taller Demo BrewLab", "Ikawa Pro V3",
                "[DEMO] Tueste claro para filtro (desarrollo 13,5%)",
                Double.valueOf(12.9), null, null, null, Double.valueOf(250.0),
                true, null, now, now
        );
        roastBatchRepository.create(batch1);

        String batch2Id = Ids.newId();
        RoastBatch batch2 = new RoastBatch(
                batch2Id, userId, lotId, "DEMO-RST-MEDIA",
                today.minusDays(3), "Media-Clara", "Taller Demo BrewLab", "Loring S7",
                "[DEMO] Tueste medio-claro con mayor dulzor",
                Double.valueOf(13.8), Double.valueOf(68.0), null, null, Double.valueOf(250.0),
                true, null, now, now
        );
        roastBatchRepository.create(batch2);

        // 3. Equipos y Perfil de Agua [DEMO]
        String grinderId = Ids.newId();
        equipmentRepository.create(new Equipment(
                grinderId, userId, "molino", "Comandante", "C40 MK4",
                "[DEMO] Comandante C40 MK4", "Clics estándar (1-40)", "Muelas Nitro Blade", true, now, now
        ));
        String dripperId = Ids.newId();
        equipmentRepository.create(new Equipment(
                dripperId, userId, "dripper", "Hario", "V60 02",
                "[DEMO] Hario V60 02 Cerámica", null, "Precalentado con agua hirviendo", true, now, now
        ));
        List<String> eqIds = Arrays.asList(grinderId, dripperId);

        String waterId = Ids.newId();
        waterProfileRepository.create(new WaterProfile(
                waterId, userId, "[DEMO] Agua Filtrada Laboratorio", "Ósmosis + remineralización",
                Double.valueOf(85.0), Double.valueOf(52.0), null, "Mg 12 mg/L, Ca 18 mg/L",
                today.minusDays(5), null, true, now, now
        ));

        // 4. Receta [DEMO] 15 g / 250 g (Ratio 1:16,7)
        String recipeId = Ids.newId();
        List<RecipeStep> steps = Arrays.asList(
                new RecipeStep(Ids.newId(), recipeId, 1, "Bloom / Preinfusión", "Verter 45 g en espiral suave", 0, 35, 45.0, 45.0, 5),
                new RecipeStep(Ids.newId(), recipeId, 2, "Primer vertido", "Verter hasta 150 g en centro y bordes", 35, 45, 105.0, 150.0, 5),
                new RecipeStep(Ids.newId(), recipeId, 3, "Segundo vertido", "Completar hasta 250 g y girar suavemente", 80, 75, 100.0, 250.0, 10)
        );
        Recipe recipe = new Recipe(
                recipeId, userId, "[DEMO] V60 Pink Bourbon 1:16,7", "V60", lotId, batch1Id,
                Double.valueOf(15.0), Double.valueOf(250.0), Double.valueOf(93.0),
                "22 clics", Integer.valueOf(150), Integer.valueOf(175),
                eqIds, waterId, "[DEMO] Receta base de filtro", 1, true, steps, now, now
        );
        recipeRepository.create(recipe);
        String snapshotJson = BrewSession.serializeStepsSnapshot(steps);

        // 5. Dos BrewSessions [DEMO] (Ensayo A con 20 clics y Ensayo B con 23 clics)
        String sessionAId = Ids.newId();
        Instant tA = now.minusSeconds(7200);
        BrewSession sessionA = new BrewSession(
                sessionAId, userId, recipeId, lotId, batch1Id, waterId, eqIds,
                BrewSession.STATUS_COMPLETED, tA, null, 0L, tA.plusSeconds(194),
                recipe.getName(), "V60", 15.0, 250.0, 93.0, "22 clics", 150, 175, snapshotJson,
                15.0, 250.0, 93.0, "20 clics", Integer.valueOf(194), 214.0,
                "[DEMO] Ensayo A: molienda más fina (20 clics), drenaje lento de 3:14",
                true,
                Arrays.asList(
                        new BrewEvent(Ids.newId(), sessionAId, null, null, BrewEvent.TYPE_STARTED, tA, 0L, 250.0, null, "START-A", "Inicio"),
                        new BrewEvent(Ids.newId(), sessionAId, steps.get(0).getId(), Integer.valueOf(1), BrewEvent.TYPE_STEP_COMPLETED, tA.plusSeconds(35), 35_000L, 45.0, 45.0, "STEP-A1", null),
                        new BrewEvent(Ids.newId(), sessionAId, steps.get(1).getId(), Integer.valueOf(2), BrewEvent.TYPE_STEP_COMPLETED, tA.plusSeconds(82), 82_000L, 150.0, 151.0, "STEP-A2", null),
                        new BrewEvent(Ids.newId(), sessionAId, steps.get(2).getId(), Integer.valueOf(3), BrewEvent.TYPE_STEP_COMPLETED, tA.plusSeconds(194), 194_000L, 250.0, 250.0, "STEP-A3", null),
                        new BrewEvent(Ids.newId(), sessionAId, null, null, BrewEvent.TYPE_COMPLETED, tA.plusSeconds(194), 194_000L, 250.0, 250.0, "COMP-A", "Finalizada")
                ),
                tA, tA.plusSeconds(194)
        );
        brewSessionRepository.create(sessionA);

        String sessionBId = Ids.newId();
        Instant tB = now.minusSeconds(3600);
        BrewSession sessionB = new BrewSession(
                sessionBId, userId, recipeId, lotId, batch1Id, waterId, eqIds,
                BrewSession.STATUS_COMPLETED, tB, null, 0L, tB.plusSeconds(162),
                recipe.getName(), "V60", 15.0, 250.0, 93.0, "22 clics", 150, 175, snapshotJson,
                15.0, 250.0, 93.0, "23 clics", Integer.valueOf(162), 216.0,
                "[DEMO] Ensayo B: molienda abierta a 23 clics, drenaje limpio en 2:42",
                true,
                Arrays.asList(
                        new BrewEvent(Ids.newId(), sessionBId, null, null, BrewEvent.TYPE_STARTED, tB, 0L, 250.0, null, "START-B", "Inicio"),
                        new BrewEvent(Ids.newId(), sessionBId, steps.get(0).getId(), Integer.valueOf(1), BrewEvent.TYPE_STEP_COMPLETED, tB.plusSeconds(35), 35_000L, 45.0, 45.0, "STEP-B1", null),
                        new BrewEvent(Ids.newId(), sessionBId, steps.get(1).getId(), Integer.valueOf(2), BrewEvent.TYPE_STEP_COMPLETED, tB.plusSeconds(80), 80_000L, 150.0, 150.0, "STEP-B2", null),
                        new BrewEvent(Ids.newId(), sessionBId, steps.get(2).getId(), Integer.valueOf(3), BrewEvent.TYPE_STEP_COMPLETED, tB.plusSeconds(162), 162_000L, 250.0, 250.0, "STEP-B3", null),
                        new BrewEvent(Ids.newId(), sessionBId, null, null, BrewEvent.TYPE_COMPLETED, tB.plusSeconds(162), 162_000L, 250.0, 250.0, "COMP-B", "Finalizada")
                ),
                tB, tB.plusSeconds(162)
        );
        brewSessionRepository.create(sessionB);

        // 6. Evaluaciones sensoriales [DEMO] con bloque afectivo separado
        String assessAId = Ids.newId();
        sensoryRepository.createAssessment(new SensoryAssessment(
                assessAId, userId, lotId, batch1Id, sessionAId,
                "Laboratorio Demo", today, "1-5",
                4.25, 3.5, 3.5, 3.25, 4.0, 3.75, 3.25, 3.5,
                "[DEMO] Aroma floral intenso, pero el drenaje largo de 3:14 aportó amargor final.",
                true,
                Arrays.asList(
                        new AssessmentDescriptor(Ids.newId(), assessAId, "bl-fl-jazmin", 4.0, "1-5", null, null),
                        new AssessmentDescriptor(Ids.newId(), assessAId, "bl-ca-nibs", 3.5, "1-5", null, null)
                ),
                new PreferenceRecord(Ids.newId(), assessAId, userId, 6.5, "1-9", "MAYBE",
                        "[DEMO] Calibración de molienda", "Prometedora en aroma, requiere abrir molienda", now),
                now, now
        ));

        String assessBId = Ids.newId();
        sensoryRepository.createAssessment(new SensoryAssessment(
                assessBId, userId, lotId, batch1Id, sessionBId,
                "Laboratorio Demo", today, "1-5",
                4.5, 4.5, 4.25, 4.5, 2.0, 3.75, 4.25, 4.5,
                "[DEMO] Taza sumamente limpia: jazmín, durazno amarillo y dulzor a panela al enfriar.",
                true,
                Arrays.asList(
                        new AssessmentDescriptor(Ids.newId(), assessBId, "bl-fl-jazmin", 4.5, "1-5", null, null),
                        new AssessmentDescriptor(Ids.newId(), assessBId, "bl-fr-durazno", 4.5, "1-5", null, null),
                        new AssessmentDescriptor(Ids.newId(), assessBId, "bl-du-panela", 4.0, "1-5", null, null)
                ),
                new PreferenceRecord(Ids.newId(), assessBId, userId, 8.8, "1-9", "YES",
                        "[DEMO] Calibración de molienda", "Excelente balance y claridad", now),
                now, now
        ));

        // 7. Experimento [DEMO] vinculando ambas sesiones
        String expId = Ids.newId();
        experimentRepository.create(new Experiment(
                expId, userId, lotId, batch1Id,
                "[DEMO] Efecto de molienda (20 vs 23 clics) en V60 Pink Bourbon",
                "¿Abrir 3 clics la molienda reduce el amargor manteniendo la intensidad floral?",
                "Optimizar la receta base V60 1:16,7 para el lote DEMO-HUI-01",
                "Con 23 clics el tiempo de drenaje entrará en la ventana 2:30–2:55 y subirá el dulzor percibido",
                "Molienda (Grind Setting)",
                "Mismo CoffeeLot DEMO-HUI-01, mismo RoastBatch DEMO-RST-CLARA, 15 g café, 250 g agua a 93 °C",
                Experiment.STATUS_CONCLUDED,
                "[DEMO] En esta prueba controlada, 23 clics redujo el tiempo de 3:14 a 2:42 y elevó el dulzor de 3,25 a 4,5.",
                true,
                Arrays.asList(
                        new ExperimentTrial(Ids.newId(), expId, sessionAId, 1, "Ensayo A (20 clics)", "20 clics", null, now),
                        new ExperimentTrial(Ids.newId(), expId, sessionBId, 2, "Ensayo B (23 clics)", "23 clics", null, now)
                ),
                now, now
        ));
    }
}
