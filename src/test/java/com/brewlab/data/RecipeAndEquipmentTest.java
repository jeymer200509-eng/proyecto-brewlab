package com.brewlab.data;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.EquipmentRepository;
import com.brewlab.data.repositories.RecipeRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.data.repositories.WaterProfileRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;
import com.brewlab.domain.model.WaterProfile;
import com.brewlab.domain.validation.RecipeValidators;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeAndEquipmentTest {

    @Test
    void equipment_water_profile_and_recipe_steps_crud_reorder_duplicate_and_coherence_warnings() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            new SchemaMigrator(mds.getDataSource()).migrate();
            Instant now = Instant.parse("2026-11-13T19:00:00Z");
            String userId = Ids.newId();
            new UserRepository(mds.getDataSource()).create(userId, "recetas@brewlab.co", "Mateo", "hash", now);

            EquipmentRepository eqRepo = new EquipmentRepository(mds.getDataSource());
            WaterProfileRepository wpRepo = new WaterProfileRepository(mds.getDataSource());
            RecipeRepository recipeRepo = new RecipeRepository(mds.getDataSource());

            Equipment grinder = new Equipment(
                    Ids.newId(), userId, "molino", "Comandante", "C40 MK4",
                    "Comandante C40", "Clics 1-40", "Eje estándar", false, now, now
            );
            eqRepo.create(grinder);
            assertEquals(1, eqRepo.findAllByUser(userId).size());

            // Perfil de agua con alcalinidad desconocida (null, nunca 0)
            WaterProfile water = new WaterProfile(
                    Ids.newId(), userId, "Agua Filtrada Lab", "Ósmosis + Mg",
                    Double.valueOf(85.0), Double.valueOf(50.0), null, null, null, null, false, now, now
            );
            wpRepo.create(water);
            WaterProfile loadedWater = wpRepo.findById(userId, water.getId());
            assertNotNull(loadedWater);
            assertEquals(Double.valueOf(85.0), loadedWater.getTdsPpm());
            assertNull(loadedWater.getAlkalinityKh(), "Alcalinidad no medida debe ser null, nunca 0.0");
            assertEquals(Formats.UNKNOWN, loadedWater.getAlkalinityDisplay());

            // Receta V60 15 g / 250 g -> 1:16,7
            String recipeId = Ids.newId();
            List<RecipeStep> steps = Arrays.asList(
                    new RecipeStep(Ids.newId(), recipeId, 1, "Preinfusión", "Verter en espiral",
                            0, Integer.valueOf(45), Double.valueOf(45.0), Double.valueOf(45.0), Integer.valueOf(5)),
                    new RecipeStep(Ids.newId(), recipeId, 2, "Primer vertido", "Verter hasta 150 g",
                            45, Integer.valueOf(45), Double.valueOf(105.0), Double.valueOf(150.0), Integer.valueOf(5)),
                    new RecipeStep(Ids.newId(), recipeId, 3, "Segundo vertido", "Verter hasta 250 g",
                            90, Integer.valueOf(45), Double.valueOf(100.0), Double.valueOf(250.0), Integer.valueOf(5))
            );
            Recipe recipe = new Recipe(
                    recipeId, userId, "V60 Lavados", "V60", null, null,
                    Double.valueOf(15.0), Double.valueOf(250.0), Double.valueOf(93.0),
                    "24 clics", Integer.valueOf(165), Integer.valueOf(185),
                    Collections.singletonList(grinder.getId()), water.getId(),
                    "Receta limpia", 1, false, steps, now, now
            );
            assertTrue(RecipeValidators.validateRecipe(recipe).isEmpty());
            assertTrue(RecipeValidators.inspectCoherenceWarnings(recipe).isEmpty(),
                    "Pasos coherentes con 250 g no deben producir advertencias");
            assertEquals("1:16,7", recipe.getRatioDisplay());

            recipeRepo.create(recipe);
            Recipe loadedRecipe = recipeRepo.findById(userId, recipeId);
            assertNotNull(loadedRecipe);
            assertEquals(3, loadedRecipe.getSteps().size());
            assertEquals(Collections.singletonList(grinder.getId()), loadedRecipe.getEquipmentIds());

            // Reordenar paso 2 hacia arriba -> queda primero
            String secondStepId = loadedRecipe.getSteps().get(1).getId();
            recipeRepo.moveStep(userId, recipeId, secondStepId, -1, now.plusSeconds(10));
            Recipe reordered = recipeRepo.findById(userId, recipeId);
            assertEquals(secondStepId, reordered.getSteps().get(0).getId());
            assertEquals(1, reordered.getSteps().get(0).getOrder());

            // Ahora que el paso de 150 g acumulados está antes que el de 45 g, debe advertir que el agua acumulada disminuye
            List<String> warningsAfterSwap = RecipeValidators.inspectCoherenceWarnings(reordered);
            assertTrue(!warningsAfterSwap.isEmpty(), "Debe advertir cuando el agua acumulada disminuye o el inicio retrocede");

            // Duplicar receta genera nuevo ID
            Recipe copy = recipeRepo.duplicate(userId, recipeId, now.plusSeconds(20));
            assertNotNull(copy);
            assertNotEquals(recipeId, copy.getId());
            assertEquals(2, copy.getVersion());
            assertEquals(3, copy.getSteps().size());
            assertNotEquals(reordered.getSteps().get(0).getId(), copy.getSteps().get(0).getId());
        }
    }
}
