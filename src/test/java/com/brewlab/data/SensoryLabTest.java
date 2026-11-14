package com.brewlab.data;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.BrewSessionRepository;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.SensoryRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.AssessmentDescriptor;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.PreferenceRecord;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.SensoryDescriptor;
import com.brewlab.domain.sensory.OriginalFlavorTaxonomy;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración para Sensory Lab y Flavor Explorer (Fase 5):
 * taxonomía propia versionada, descriptores personalizados, separación de intensidad/escala,
 * preservación de null (nunca 0) y bloque afectivo separado (PreferenceRecord).
 */
class SensoryLabTest {

    @Test
    void originalTaxonomyCustomDescriptorsAndSensoryAssessmentWithSeparatedPreference() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            DataSource ds = mds.getDataSource();
            new SchemaMigrator(ds).migrate();

            Instant now = Instant.parse("2026-04-12T15:00:00Z");
            String userId = Ids.newId();
            new UserRepository(ds).create(userId, "sensory@brewlab.local", "Elena", "hash", now);

            SensoryRepository sensoryRepo = new SensoryRepository(ds);
            // Sembrar dos veces para comprobar idempotencia
            sensoryRepo.seedDefaultTaxonomyIfNeeded();
            sensoryRepo.seedDefaultTaxonomyIfNeeded();

            List<SensoryDescriptor> baseDescriptors = sensoryRepo.findAllDescriptors(userId);
            assertEquals(OriginalFlavorTaxonomy.defaultDescriptors().size(), baseDescriptors.size());
            assertTrue(baseDescriptors.size() >= 30);

            // Verificar generación geométrica de sectores SVG para la rueda concéntrica original
            List<OriginalFlavorTaxonomy.WheelSector> familySectors =
                    OriginalFlavorTaxonomy.buildFamilySectors(baseDescriptors);
            List<OriginalFlavorTaxonomy.WheelSector> subfamilySectors =
                    OriginalFlavorTaxonomy.buildSubfamilySectors(baseDescriptors);
            assertEquals(7, familySectors.size());
            assertFalse(subfamilySectors.isEmpty());

            // Crear descriptor personalizado jerárquico
            String customDescId = Ids.newId();
            SensoryDescriptor customLulo = new SensoryDescriptor(
                    customDescId, userId, "Frutal Luminoso", "Frutas andinas",
                    "Lulo / Naranjilla", "#E29526", OriginalFlavorTaxonomy.VERSION, true, now
            );
            sensoryRepo.createCustomDescriptor(customLulo);

            List<SensoryDescriptor> withCustom = sensoryRepo.findAllDescriptors(userId);
            assertEquals(baseDescriptors.size() + 1, withCustom.size());

            // Crear lote, tueste y sesión para vincular la evaluación
            String lotId = Ids.newId();
            new CoffeeLotRepository(ds).create(new CoffeeLot(
                    lotId, userId, "COL-NAR-09", "Nariño Buesaco", "Colombia", "Nariño", "Buesaco",
                    "El Mirador", "Familia Ortiz", "Arabica", "Caturra", "Lavado",
                    Integer.valueOf(2050), null, "2026", null, null, null, null, null, false, null, now, now
            ));
            String batchId = Ids.newId();
            new RoastBatchRepository(ds).create(new RoastBatch(
                    batchId, userId, lotId, "T-NAR-01", LocalDate.of(2026, 4, 5),
                    "Clara", "Lab Roaster", null, null, null, null, null, null, null, false, null, now, now
            ));
            String sessionId = Ids.newId();
            new BrewSessionRepository(ds).create(new BrewSession(
                    sessionId, userId, null, lotId, batchId, null, Collections.<String>emptyList(),
                    BrewSession.STATUS_COMPLETED, now.minusSeconds(160), null, 0L, now,
                    "V60 Nariño", "V60", 15.0, 250.0, 93.0, "21 clics", 150, 175, "[]",
                    15.0, 250.0, 93.0, "21 clics", Integer.valueOf(160), 215.0, null,
                    false, Collections.<BrewEvent>emptyList(), now, now
            ));

            // Evaluación sensorial descriptiva (dejando bitterness y aftertaste como null)
            String assessmentId = Ids.newId();
            List<AssessmentDescriptor> chosenDescriptors = Arrays.asList(
                    new AssessmentDescriptor(Ids.newId(), assessmentId, "bl-fl-jazmin", Double.valueOf(4.0), "1-5", "En fragancia", null),
                    new AssessmentDescriptor(Ids.newId(), assessmentId, customDescId, Double.valueOf(4.5), "1-5", "Acidez jugosa", null)
            );
            PreferenceRecord preference = new PreferenceRecord(
                    Ids.newId(), assessmentId, userId, Double.valueOf(8.5), "1-9",
                    "YES", "Cata comparativa de tarde", "Taza sumamente brillante y limpia", now
            );
            SensoryAssessment assessment = new SensoryAssessment(
                    assessmentId, userId, lotId, batchId, sessionId,
                    "Elena", LocalDate.of(2026, 4, 12), "1-5",
                    Double.valueOf(4.5), Double.valueOf(4.25), Double.valueOf(4.5), Double.valueOf(4.0),
                    null, // bitterness no evaluado -> debe persistir como null, nunca 0
                    Double.valueOf(3.5),
                    null, // aftertaste no evaluado -> debe persistir como null, nunca 0
                    Double.valueOf(4.25),
                    "Aroma floral intenso con acidez cítrica de lulo al enfriar.",
                    false, chosenDescriptors, preference, now, now
            );
            sensoryRepo.createAssessment(assessment);

            SensoryAssessment loaded = sensoryRepo.findById(userId, assessmentId);
            assertNotNull(loaded);
            assertEquals("1-5", loaded.getAttributeScale());
            assertEquals(Double.valueOf(4.5), loaded.getFragranceAroma());
            assertNull(loaded.getBitterness(), "Amargor no evaluado debe ser null, nunca 0");
            assertEquals(Formats.UNKNOWN, loaded.getBitternessDisplay());
            assertNull(loaded.getAftertaste(), "Posgusto no evaluado debe ser null, nunca 0");
            assertEquals(Formats.UNKNOWN, loaded.getAftertasteDisplay());

            // Verificar descriptores con descriptorId, intensidad y escala por separado
            assertEquals(2, loaded.getDescriptors().size());
            boolean foundCustom = loaded.getDescriptors().stream()
                    .anyMatch(ad -> customDescId.equals(ad.getDescriptorId())
                            && Double.valueOf(4.5).equals(ad.getIntensity())
                            && "1-5".equals(ad.getScale())
                            && ad.getDescriptor().isCustom());
            assertTrue(foundCustom);

            // Verificar bloque afectivo separado (PreferenceRecord)
            assertNotNull(loaded.getPreferenceRecord());
            assertEquals(Double.valueOf(8.5), loaded.getPreferenceRecord().getOverallLiking());
            assertEquals("1-9", loaded.getPreferenceRecord().getLikingScale());
            assertEquals("YES", loaded.getPreferenceRecord().getWouldRepeat());

            // Consulta por sesión de preparación
            assertEquals(1, sensoryRepo.findByBrewSessionId(userId, sessionId).size());
        }
    }
}
