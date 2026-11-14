package com.brewlab.data;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.BrewSessionRepository;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.ExperimentRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.SensoryRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.analytics.RuleRecommendationEngine;
import com.brewlab.domain.analytics.SessionComparison;
import com.brewlab.domain.model.AssessmentDescriptor;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Experiment;
import com.brewlab.domain.model.ExperimentTrial;
import com.brewlab.domain.model.PreferenceRecord;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias y de integración para Analítica, Comparador de Sesiones (SessionComparison),
 * Motor de Reglas Explicable (RuleRecommendationEngine) y Experiment Lab (ExperimentRepository).
 */
class AnalyticsAndExperimentTest {

    @Test
    void sessionComparisonDetectsSingleVsMultipleChangedVariablesAndRuleEngineSuggestsHypotheses() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            DataSource ds = mds.getDataSource();
            new SchemaMigrator(ds).migrate();

            Instant now = Instant.parse("2026-04-13T16:00:00Z");
            String userId = Ids.newId();
            new UserRepository(ds).create(userId, "exp@brewlab.local", "Camilo", "hash", now);

            SensoryRepository sensoryRepo = new SensoryRepository(ds);
            sensoryRepo.seedDefaultTaxonomyIfNeeded();

            String lotId = Ids.newId();
            new CoffeeLotRepository(ds).create(new CoffeeLot(
                    lotId, userId, "COL-TOL-05", "Tolima Planadas", "Colombia", "Tolima", "Planadas",
                    "La Roca", "Pedro Ruiz", "Arabica", "Tabi", "Natural",
                    Integer.valueOf(1900), null, "2026", null, null, null, null, null, false, null, now, now
            ));
            String batchId = Ids.newId();
            new RoastBatchRepository(ds).create(new RoastBatch(
                    batchId, userId, lotId, "T-TOL-01", LocalDate.of(2026, 4, 3),
                    "Clara", "Roaster Lab", null, null, null, null, null, null, null, false, null, now, now
            ));

            BrewSessionRepository sessionRepo = new BrewSessionRepository(ds);

            // Sesión A: 20 clics (molienda fina), tiempo 210s (> máx 175s), amargor alto (4.2 / 5)
            String sessionAId = Ids.newId();
            BrewSession sA = new BrewSession(
                    sessionAId, userId, null, lotId, batchId, null, Collections.<String>emptyList(),
                    BrewSession.STATUS_COMPLETED, now.minusSeconds(210), null, 0L, now,
                    "V60 Tabi", "V60", 15.0, 250.0, 93.0, "20 clics", 150, 175, "[]",
                    15.0, 250.0, 93.0, "20 clics", Integer.valueOf(210), 212.0, "Drenaje lento",
                    false, Collections.<BrewEvent>emptyList(), now, now
            );
            sessionRepo.create(sA);

            SensoryAssessment aA = new SensoryAssessment(
                    Ids.newId(), userId, lotId, batchId, sessionAId,
                    "Camilo", LocalDate.of(2026, 4, 13), "1-5",
                    4.0, 3.5, 3.25, 3.0, 4.2, 3.75, null, 3.25,
                    "Amargor marcado al final", false,
                    Collections.<AssessmentDescriptor>emptyList(),
                    new PreferenceRecord(Ids.newId(), sessionAId, userId, 6.0, "1-9", "MAYBE", null, null, now),
                    now, now
            );
            sensoryRepo.createAssessment(aA);

            // Sesión B: único cambio = molienda a 24 clics, tiempo 162s, mayor dulzor y balance
            String sessionBId = Ids.newId();
            BrewSession sB = new BrewSession(
                    sessionBId, userId, null, lotId, batchId, null, Collections.<String>emptyList(),
                    BrewSession.STATUS_COMPLETED, now.minusSeconds(162), null, 0L, now,
                    "V60 Tabi", "V60", 15.0, 250.0, 93.0, "24 clics", 150, 175, "[]",
                    15.0, 250.0, 93.0, "24 clics", Integer.valueOf(162), null, "Extracción fluida",
                    false, Collections.<BrewEvent>emptyList(), now, now
            );
            sessionRepo.create(sB);

            SensoryAssessment aB = new SensoryAssessment(
                    Ids.newId(), userId, lotId, batchId, sessionBId,
                    "Camilo", LocalDate.of(2026, 4, 13), "1-5",
                    4.5, 4.5, 4.25, 4.5, 2.0, 3.75, 4.25, 4.5,
                    "Taza limpia y dulce", false,
                    Collections.<AssessmentDescriptor>emptyList(),
                    new PreferenceRecord(Ids.newId(), sessionBId, userId, 8.5, "1-9", "YES", null, null, now),
                    now, now
            );
            sensoryRepo.createAssessment(aB);

            Map<String, SensoryAssessment> assessMap = new HashMap<>();
            assessMap.put(sessionAId, aA);
            assessMap.put(sessionBId, aB);

            // 1. Comparar S_A y S_B donde solo cambió el ajuste de molienda
            SessionComparison cleanComp = SessionComparison.build(Arrays.asList(sA, sB), assessMap);
            assertEquals(2, cleanComp.getSampleSize());
            assertEquals(1, cleanComp.getChangedVariables().size());
            assertEquals("Ajuste de molienda", cleanComp.getChangedVariables().get(0));
            assertTrue(cleanComp.getContextWarnings().isEmpty(),
                    "Con una sola variable modificada y misma escala no debe emitir advertencia de variables múltiples");

            // Verificar que el motor de reglas detecta R-01 en la Sesión A (tiempo > máx + amargor alto)
            List<RuleRecommendationEngine.RuleSuggestion> rulesA = RuleRecommendationEngine.evaluate(sA, aA);
            assertFalse(rulesA.isEmpty());
            assertTrue(rulesA.stream().anyMatch(r -> "R-01".equals(r.getRuleCode())));

            // 2. Persistir Experimento con Ensayo A y Ensayo B y concluirlo
            ExperimentRepository expRepo = new ExperimentRepository(ds);
            String expId = Ids.newId();
            Experiment exp = new Experiment(
                    expId, userId, lotId, batchId,
                    "Curva de molienda en V60 Tabi Natural",
                    "¿Abrir 4 clics reduce el amargor sin perder cuerpo?",
                    "Encontrar el punto dulce del lote Tolima",
                    "Con 24 clics el drenaje se ubicará en ~160 s y subirá el dulzor",
                    "Molienda (Grind Setting)",
                    "Mismo lote, mismo tueste, 15 g café, 250 g agua a 93 °C",
                    Experiment.STATUS_OPEN, null, false,
                    Collections.singletonList(
                            new ExperimentTrial(Ids.newId(), expId, sessionAId, 1, "Ensayo A", "20 clics", null, now)
                    ),
                    now, now
            );
            expRepo.create(exp);

            Experiment withSecondTrial = expRepo.addTrial(
                    userId, expId,
                    new ExperimentTrial(Ids.newId(), expId, sessionBId, 2, "Ensayo B", "24 clics", null, now),
                    now.plusSeconds(30)
            );
            assertNotNull(withSecondTrial);
            assertEquals(2, withSecondTrial.getTrials().size());

            Experiment concluded = expRepo.updateConclusionAndStatus(
                    userId, expId,
                    "Se confirma la hipótesis en este lote: 24 clics redujo el tiempo a 2:42 y elevó el balance a 4,5.",
                    Experiment.STATUS_CONCLUDED, now.plusSeconds(60)
            );
            assertEquals(Experiment.STATUS_CONCLUDED, concluded.getStatus());
            assertTrue(concluded.getUserConclusion().contains("24 clics"));

            // 3. Si comparamos con una tercera sesión que cambia temperatura Y dosis, debe advertir variables múltiples
            BrewSession sC = new BrewSession(
                    Ids.newId(), userId, null, lotId, batchId, null, Collections.<String>emptyList(),
                    BrewSession.STATUS_COMPLETED, now.minusSeconds(150), null, 0L, now,
                    "AeroPress Tabi", "AeroPress", 18.0, 220.0, 88.0, "18 clics", 120, 150, "[]",
                    18.0, 220.0, 88.0, "18 clics", Integer.valueOf(140), null, null,
                    false, Collections.<BrewEvent>emptyList(), now, now
            );
            SessionComparison multiVarComp = SessionComparison.build(Arrays.asList(sA, sC), assessMap);
            assertTrue(multiVarComp.getChangedVariables().size() > 1);
            assertFalse(multiVarComp.getContextWarnings().isEmpty(),
                    "Debe advertir explícitamente cuando cambian múltiples variables simultáneamente");
        }
    }
}
