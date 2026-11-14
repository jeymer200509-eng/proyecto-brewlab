package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RecipeStep;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Repositorio JDBC para {@link Recipe} y sus {@link RecipeStep} ordenados. */
public final class RecipeRepository {
    private final DataSource dataSource;

    public RecipeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(Recipe recipe) {
        String sql = "INSERT INTO recipes (id, user_id, name, method, coffee_lot_id, roast_batch_id, "
                + "dose_g, water_g, temperature_c, grind_setting, target_time_min_s, target_time_max_s, "
                + "water_profile_id, notes, version, is_demo, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, recipe.getId());
                    ps.setString(2, recipe.getUserId());
                    ps.setString(3, recipe.getName());
                    ps.setString(4, recipe.getMethod());
                    ps.setString(5, recipe.getCoffeeLotId());
                    ps.setString(6, recipe.getRoastBatchId());
                    JdbcUtils.setNullableDouble(ps, 7, recipe.getDoseG());
                    JdbcUtils.setNullableDouble(ps, 8, recipe.getWaterG());
                    JdbcUtils.setNullableDouble(ps, 9, recipe.getTemperatureC());
                    ps.setString(10, recipe.getGrindSetting());
                    JdbcUtils.setNullableInt(ps, 11, recipe.getTargetTimeMinS());
                    JdbcUtils.setNullableInt(ps, 12, recipe.getTargetTimeMaxS());
                    ps.setString(13, recipe.getWaterProfileId());
                    ps.setString(14, recipe.getNotes());
                    ps.setInt(15, recipe.getVersion());
                    ps.setBoolean(16, recipe.isDemo());
                    JdbcUtils.setNullableTimestamp(ps, 17, recipe.getCreatedAt());
                    JdbcUtils.setNullableTimestamp(ps, 18, recipe.getUpdatedAt());
                    ps.executeUpdate();
                }
                replaceEquipmentLinks(c, recipe.getId(), recipe.getEquipmentIds());
                replaceSteps(c, recipe.getId(), recipe.getSteps());
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la receta", e);
        }
    }

    public void updateHeaderAndEquipment(Recipe recipe) {
        String sql = "UPDATE recipes SET name = ?, method = ?, coffee_lot_id = ?, roast_batch_id = ?, "
                + "dose_g = ?, water_g = ?, temperature_c = ?, grind_setting = ?, target_time_min_s = ?, "
                + "target_time_max_s = ?, water_profile_id = ?, notes = ?, version = ?, is_demo = ?, updated_at = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, recipe.getName());
                    ps.setString(2, recipe.getMethod());
                    ps.setString(3, recipe.getCoffeeLotId());
                    ps.setString(4, recipe.getRoastBatchId());
                    JdbcUtils.setNullableDouble(ps, 5, recipe.getDoseG());
                    JdbcUtils.setNullableDouble(ps, 6, recipe.getWaterG());
                    JdbcUtils.setNullableDouble(ps, 7, recipe.getTemperatureC());
                    ps.setString(8, recipe.getGrindSetting());
                    JdbcUtils.setNullableInt(ps, 9, recipe.getTargetTimeMinS());
                    JdbcUtils.setNullableInt(ps, 10, recipe.getTargetTimeMaxS());
                    ps.setString(11, recipe.getWaterProfileId());
                    ps.setString(12, recipe.getNotes());
                    ps.setInt(13, recipe.getVersion());
                    ps.setBoolean(14, recipe.isDemo());
                    JdbcUtils.setNullableTimestamp(ps, 15, recipe.getUpdatedAt());
                    ps.setString(16, recipe.getId());
                    ps.setString(17, recipe.getUserId());
                    ps.executeUpdate();
                }
                replaceEquipmentLinks(c, recipe.getId(), recipe.getEquipmentIds());
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la receta", e);
        }
    }

    public void delete(String userId, String recipeId) {
        String sql = "DELETE FROM recipes WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recipeId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar la receta", e);
        }
    }

    public Recipe findById(String userId, String recipeId) {
        if (recipeId == null || userId == null) {
            return null;
        }
        String sql = "SELECT * FROM recipes WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, recipeId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                List<String> eqIds = loadEquipmentIds(c, recipeId);
                List<RecipeStep> steps = loadSteps(c, recipeId);
                return mapRecipe(rs, eqIds, steps);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando receta", e);
        }
    }

    public List<Recipe> findAllByUser(String userId) {
        String sql = "SELECT * FROM recipes WHERE user_id = ? ORDER BY updated_at DESC, created_at DESC";
        List<Recipe> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rId = rs.getString("id");
                    List<String> eqIds = loadEquipmentIds(c, rId);
                    List<RecipeStep> steps = loadSteps(c, rId);
                    list.add(mapRecipe(rs, eqIds, steps));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando recetas", e);
        }
    }

    /** Duplica una receta generando un nuevo ID de receta y nuevos IDs para todos sus pasos. */
    public Recipe duplicate(String userId, String sourceRecipeId, Instant now) {
        Recipe orig = findById(userId, sourceRecipeId);
        if (orig == null) {
            return null;
        }
        String newRecipeId = Ids.newId();
        int nextVersion = orig.getVersion() + 1;
        List<RecipeStep> copiedSteps = new ArrayList<>(orig.getSteps().size());
        int order = 1;
        for (RecipeStep s : orig.getSteps()) {
            copiedSteps.add(new RecipeStep(
                    Ids.newId(), newRecipeId, order++,
                    s.getTitle(), s.getInstruction(),
                    s.getStartSeconds(), s.getDurationSeconds(),
                    s.getWaterDoseG(), s.getCumulativeWaterG(), s.getToleranceSeconds()
            ));
        }
        Recipe copy = new Recipe(
                newRecipeId, userId, orig.getName() + " (Copia v" + nextVersion + ")",
                orig.getMethod(), orig.getCoffeeLotId(), orig.getRoastBatchId(),
                orig.getDoseG(), orig.getWaterG(), orig.getTemperatureC(),
                orig.getGrindSetting(), orig.getTargetTimeMinS(), orig.getTargetTimeMaxS(),
                orig.getEquipmentIds(), orig.getWaterProfileId(),
                orig.getNotes(), nextVersion, false, copiedSteps, now, now
        );
        create(copy);
        return copy;
    }

    public void saveStep(String userId, String recipeId, RecipeStep step, Instant now) {
        Recipe recipe = findById(userId, recipeId);
        if (recipe == null) {
            throw new IllegalStateException("Receta no encontrada");
        }
        List<RecipeStep> current = new ArrayList<>(recipe.getSteps());
        boolean replaced = false;
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getId().equals(step.getId())) {
                current.set(i, new RecipeStep(
                        step.getId(), recipeId, i + 1,
                        step.getTitle(), step.getInstruction(), step.getStartSeconds(),
                        step.getDurationSeconds(), step.getWaterDoseG(),
                        step.getCumulativeWaterG(), step.getToleranceSeconds()
                ));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            current.add(new RecipeStep(
                    step.getId() != null ? step.getId() : Ids.newId(),
                    recipeId, current.size() + 1,
                    step.getTitle(), step.getInstruction(), step.getStartSeconds(),
                    step.getDurationSeconds(), step.getWaterDoseG(),
                    step.getCumulativeWaterG(), step.getToleranceSeconds()
            ));
        }
        persistNormalizedSteps(recipeId, current, now);
    }

    public void deleteStep(String userId, String recipeId, String stepId, Instant now) {
        Recipe recipe = findById(userId, recipeId);
        if (recipe == null) {
            return;
        }
        List<RecipeStep> remaining = new ArrayList<>();
        for (RecipeStep s : recipe.getSteps()) {
            if (!s.getId().equals(stepId)) {
                remaining.add(s);
            }
        }
        persistNormalizedSteps(recipeId, remaining, now);
    }

    public void duplicateStep(String userId, String recipeId, String stepId, Instant now) {
        Recipe recipe = findById(userId, recipeId);
        if (recipe == null) {
            return;
        }
        List<RecipeStep> updated = new ArrayList<>();
        for (RecipeStep s : recipe.getSteps()) {
            updated.add(s);
            if (s.getId().equals(stepId)) {
                updated.add(new RecipeStep(
                        Ids.newId(), recipeId, s.getOrder() + 1,
                        s.getTitle() + " (copia)", s.getInstruction(),
                        s.getStartSeconds(), s.getDurationSeconds(),
                        s.getWaterDoseG(), s.getCumulativeWaterG(), s.getToleranceSeconds()
                ));
            }
        }
        persistNormalizedSteps(recipeId, updated, now);
    }

    public void moveStep(String userId, String recipeId, String stepId, int delta, Instant now) {
        Recipe recipe = findById(userId, recipeId);
        if (recipe == null) {
            return;
        }
        List<RecipeStep> steps = new ArrayList<>(recipe.getSteps());
        int idx = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(stepId)) {
                idx = i;
                break;
            }
        }
        int targetIdx = idx + delta;
        if (idx < 0 || targetIdx < 0 || targetIdx >= steps.size()) {
            return;
        }
        Collections.swap(steps, idx, targetIdx);
        persistNormalizedSteps(recipeId, steps, now);
    }

    private void persistNormalizedSteps(String recipeId, List<RecipeStep> rawSteps, Instant now) {
        List<RecipeStep> normalized = new ArrayList<>(rawSteps.size());
        for (int i = 0; i < rawSteps.size(); i++) {
            RecipeStep s = rawSteps.get(i);
            normalized.add(new RecipeStep(
                    s.getId(), recipeId, i + 1,
                    s.getTitle(), s.getInstruction(), s.getStartSeconds(),
                    s.getDurationSeconds(), s.getWaterDoseG(),
                    s.getCumulativeWaterG(), s.getToleranceSeconds()
            ));
        }
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                replaceSteps(c, recipeId, normalized);
                try (PreparedStatement up = c.prepareStatement("UPDATE recipes SET updated_at = ? WHERE id = ?")) {
                    JdbcUtils.setNullableTimestamp(up, 1, now);
                    up.setString(2, recipeId);
                    up.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error guardando pasos de la receta", e);
        }
    }

    private static void replaceEquipmentLinks(Connection c, String recipeId, List<String> equipmentIds)
            throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM recipe_equipment WHERE recipe_id = ?")) {
            del.setString(1, recipeId);
            del.executeUpdate();
        }
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return;
        }
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO recipe_equipment (recipe_id, equipment_id) VALUES (?, ?)")) {
            for (String eqId : equipmentIds) {
                if (eqId != null && !eqId.trim().isEmpty()) {
                    ins.setString(1, recipeId);
                    ins.setString(2, eqId.trim());
                    ins.addBatch();
                }
            }
            ins.executeBatch();
        }
    }

    private static void replaceSteps(Connection c, String recipeId, List<RecipeStep> steps)
            throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM recipe_steps WHERE recipe_id = ?")) {
            del.setString(1, recipeId);
            del.executeUpdate();
        }
        if (steps == null || steps.isEmpty()) {
            return;
        }
        String insSql = "INSERT INTO recipe_steps (id, recipe_id, step_order, title, instruction, "
                + "start_seconds, duration_seconds, water_dose_g, cumulative_water_g, tolerance_seconds) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ins = c.prepareStatement(insSql)) {
            int order = 1;
            for (RecipeStep s : steps) {
                ins.setString(1, s.getId() != null ? s.getId() : Ids.newId());
                ins.setString(2, recipeId);
                ins.setInt(3, order++);
                ins.setString(4, s.getTitle());
                ins.setString(5, s.getInstruction());
                ins.setInt(6, s.getStartSeconds());
                JdbcUtils.setNullableInt(ins, 7, s.getDurationSeconds());
                JdbcUtils.setNullableDouble(ins, 8, s.getWaterDoseG());
                JdbcUtils.setNullableDouble(ins, 9, s.getCumulativeWaterG());
                JdbcUtils.setNullableInt(ins, 10, s.getToleranceSeconds());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    private static List<String> loadEquipmentIds(Connection c, String recipeId) throws SQLException {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT equipment_id FROM recipe_equipment WHERE recipe_id = ?")) {
            ps.setString(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("equipment_id"));
                }
            }
        }
        return ids;
    }

    private static List<RecipeStep> loadSteps(Connection c, String recipeId) throws SQLException {
        List<RecipeStep> steps = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM recipe_steps WHERE recipe_id = ? ORDER BY step_order ASC")) {
            ps.setString(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    steps.add(new RecipeStep(
                            rs.getString("id"),
                            rs.getString("recipe_id"),
                            rs.getInt("step_order"),
                            rs.getString("title"),
                            rs.getString("instruction"),
                            rs.getInt("start_seconds"),
                            JdbcUtils.getNullableInt(rs, "duration_seconds"),
                            JdbcUtils.getNullableDouble(rs, "water_dose_g"),
                            JdbcUtils.getNullableDouble(rs, "cumulative_water_g"),
                            JdbcUtils.getNullableInt(rs, "tolerance_seconds")
                    ));
                }
            }
        }
        return steps;
    }

    private static Recipe mapRecipe(ResultSet rs, List<String> eqIds, List<RecipeStep> steps)
            throws SQLException {
        return new Recipe(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("method"),
                rs.getString("coffee_lot_id"),
                rs.getString("roast_batch_id"),
                JdbcUtils.getNullableDouble(rs, "dose_g"),
                JdbcUtils.getNullableDouble(rs, "water_g"),
                JdbcUtils.getNullableDouble(rs, "temperature_c"),
                rs.getString("grind_setting"),
                JdbcUtils.getNullableInt(rs, "target_time_min_s"),
                JdbcUtils.getNullableInt(rs, "target_time_max_s"),
                eqIds,
                rs.getString("water_profile_id"),
                rs.getString("notes"),
                rs.getInt("version"),
                rs.getBoolean("is_demo"),
                steps,
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }
}
