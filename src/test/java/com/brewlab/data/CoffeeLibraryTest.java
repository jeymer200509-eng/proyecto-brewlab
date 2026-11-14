package com.brewlab.data;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.calc.RoastAge;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.validation.CoffeeValidators;
import com.brewlab.domain.validation.FieldErrors;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoffeeLibraryTest {

    @Test
    void coffee_lot_and_multiple_roast_batches_crud_search_archive_and_null_integrity() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            new SchemaMigrator(mds.getDataSource()).migrate();
            Instant now = Instant.parse("2026-11-13T18:00:00Z");
            String userId = Ids.newId();
            new UserRepository(mds.getDataSource()).create(userId, "barista@brewlab.co", "Camila", "hash", now);

            CoffeeLotRepository lotRepo = new CoffeeLotRepository(mds.getDataSource());
            RoastBatchRepository batchRepo = new RoastBatchRepository(mds.getDataSource());

            String lotId = Ids.newId();
            CoffeeLot lot = new CoffeeLot(
                    lotId, userId, "COL-HUI-01", "Finca La Esperanza — Pink Bourbon",
                    "Colombia", "Huila", "Palestina", "La Esperanza", "Jorge Gómez",
                    "Coffea arabica", "Pink Bourbon", "Lavado",
                    Integer.valueOf(1750), null, "2026-A", null,
                    null, null, null, "Lote experimental lavado con fermentación de 36 h",
                    false, null, now, now
            );
            assertTrue(CoffeeValidators.validateCoffeeLot(lot).isEmpty());
            lotRepo.create(lot);

            CoffeeLot loaded = lotRepo.findById(userId, lotId);
            assertNotNull(loaded);
            assertEquals("COL-HUI-01", loaded.getCode());
            assertEquals(Integer.valueOf(1750), loaded.getAltitudeMin());
            assertNull(loaded.getAltitudeMax(), "Dato ausente debe conservarse como null, nunca 0");
            assertNull(loaded.getMoisturePct(), "Humedad ausente debe conservarse como null, nunca 0.0");
            assertNull(loaded.getDensity(), "Densidad ausente debe conservarse como null, nunca 0.0");
            assertEquals(Formats.UNKNOWN, loaded.getMoistureDisplay());
            assertEquals("1750\u00a0m", loaded.getAltitudeDisplay());

            // Búsqueda por variedad y origen
            assertEquals(1, lotRepo.findAll(userId, "bourbon", false).size());
            assertEquals(1, lotRepo.findAll(userId, "huila", false).size());
            assertEquals(0, lotRepo.findAll(userId, "etiopia", false).size());

            // Relación 1:N: dos RoastBatch para el mismo CoffeeLot con edades distintas
            LocalDate today = LocalDate.of(2026, 11, 13);
            RoastBatch batch1 = new RoastBatch(
                    Ids.newId(), userId, lotId, "T-01-CLARA",
                    today.minusDays(8), "Clara", "Taller Café", "Ikawa Pro",
                    "Perfil floral", Double.valueOf(13.2), null, null, null, null,
                    false, null, now, now
            );
            RoastBatch batch2 = new RoastBatch(
                    Ids.newId(), userId, lotId, "T-02-MEDIA",
                    today, "Media", "Taller Café", "Loring S7",
                    "Más dulzor", null, Double.valueOf(65.0), null, null, Double.valueOf(250.0),
                    false, null, now, now
            );
            assertTrue(CoffeeValidators.validateRoastBatch(batch1).isEmpty());
            assertTrue(CoffeeValidators.validateRoastBatch(batch2).isEmpty());
            batchRepo.create(batch1);
            batchRepo.create(batch2);

            List<RoastBatch> batches = batchRepo.findByCoffeeLot(userId, lotId, false);
            assertEquals(2, batches.size());

            // Verificar edades calculadas y preservación de null
            RoastBatch reloadedB1 = batchRepo.findById(userId, batch1.getId());
            RoastBatch reloadedB2 = batchRepo.findById(userId, batch2.getId());
            assertEquals(Long.valueOf(8L), reloadedB1.ageAt(today).getDays());
            assertNull(reloadedB1.getAgtron(), "Agtron no medido debe ser null, nunca 0");
            assertEquals(Formats.UNKNOWN, reloadedB1.getAgtronDisplay());

            assertEquals(Long.valueOf(0L), reloadedB2.ageAt(today).getDays());
            assertNull(reloadedB2.getWeightLossPct(), "Merma no medida debe ser null, nunca 0");

            // Fecha futura advierte
            RoastBatch futureBatch = new RoastBatch(
                    Ids.newId(), userId, lotId, "T-03-FUTURA",
                    today.plusDays(2), "Clara", null, null, null,
                    null, null, null, null, null, false, null, now, now
            );
            RoastAge futureAge = futureBatch.ageAt(today);
            assertTrue(futureAge.isFuture());
            assertEquals(RoastAge.Status.FUTURE_DATE, futureAge.getStatus());

            // Archivar y restaurar lote
            CoffeeLot archivedLot = new CoffeeLot(
                    loaded.getId(), loaded.getUserId(), loaded.getCode(), loaded.getName(),
                    loaded.getCountry(), loaded.getRegion(), loaded.getMunicipality(),
                    loaded.getFarm(), loaded.getProducer(), loaded.getSpecies(), loaded.getVariety(),
                    loaded.getProcess(), loaded.getAltitudeMin(), Integer.valueOf(1850),
                    loaded.getHarvest(), loaded.getReceivedOn(), loaded.getMoisturePct(),
                    loaded.getDensity(), loaded.getScreenSize(), loaded.getNotes(),
                    false, now.plusSeconds(60), loaded.getCreatedAt(), now.plusSeconds(60)
            );
            lotRepo.update(archivedLot);
            assertEquals(0, lotRepo.findAll(userId, null, false).size(), "Archivado no aparece por defecto");
            assertEquals(1, lotRepo.findAll(userId, null, true).size(), "Archivado aparece si includeArchived=true");

            // Validación detecta rango de altitud invertido
            CoffeeLot invalidAlt = new CoffeeLot(
                    Ids.newId(), userId, "C1", "Café", null, null, null, null, null, null, null, null,
                    Integer.valueOf(2000), Integer.valueOf(1500), null, null, null, null, null, null,
                    false, null, now, now
            );
            FieldErrors errs = CoffeeValidators.validateCoffeeLot(invalidAlt);
            assertTrue(errs.has("altitudeMax"));
        }
    }
}
