package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.Ids;
import com.brewlab.domain.calc.BrewTimerEngine;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.RecipeStep;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio JDBC para {@link BrewSession} y {@link BrewEvent} con protección idempotente
 * contra duplicación de eventos por doble clic o reenvío de formularios.
 */
public final class BrewSessionRepository {
    private final DataSource dataSource;

    public BrewSessionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(BrewSession session) {
        String sql = "INSERT INTO brew_sessions (id, user_id, recipe_id, coffee_lot_id, roast_batch_id, "
                + "water_profile_id, status, started_at, paused_at, accumulated_pause_ms, completed_at, "
                + "planned_recipe_name, planned_method, planned_dose_g, planned_water_g, planned_temperature_c, "
                + "planned_grind_setting, planned_target_time_min_s, planned_target_time_max_s, recipe_snapshot_json, "
                + "actual_dose_g, actual_water_g, actual_temperature_c, actual_grind_setting, total_time_seconds, "
                + "final_beverage_weight_g, notes, is_demo, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, session.getId());
                    ps.setString(2, session.getUserId());
                    ps.setString(3, session.getRecipeId());
                    ps.setString(4, session.getCoffeeLotId());
                    ps.setString(5, session.getRoastBatchId());
                    ps.setString(6, session.getWaterProfileId());
                    ps.setString(7, session.getStatus());
                    JdbcUtils.setNullableTimestamp(ps, 8, session.getStartedAt());
                    JdbcUtils.setNullableTimestamp(ps, 9, session.getPausedAt());
                    ps.setLong(10, session.getAccumulatedPauseMs());
                    JdbcUtils.setNullableTimestamp(ps, 11, session.getCompletedAt());
                    ps.setString(12, session.getPlannedRecipeName());
                    ps.setString(13, session.getPlannedMethod());
                    JdbcUtils.setNullableDouble(ps, 14, session.getPlannedDoseG());
                    JdbcUtils.setNullableDouble(ps, 15, session.getPlannedWaterG());
                    JdbcUtils.setNullableDouble(ps, 16, session.getPlannedTemperatureC());
                    ps.setString(17, session.getPlannedGrindSetting());
                    JdbcUtils.setNullableInt(ps, 18, session.getPlannedTargetTimeMinS());
                    JdbcUtils.setNullableInt(ps, 19, session.getPlannedTargetTimeMaxS());
                    ps.setString(20, session.getRecipeSnapshotJson());
                    JdbcUtils.setNullableDouble(ps, 21, session.getActualDoseG());
                    JdbcUtils.setNullableDouble(ps, 22, session.getActualWaterG());
                    JdbcUtils.setNullableDouble(ps, 23, session.getActualTemperatureC());
                    ps.setString(24, session.getActualGrindSetting());
                    JdbcUtils.setNullableInt(ps, 25, session.getTotalTimeSeconds());
                    JdbcUtils.setNullableDouble(ps, 26, session.getFinalBeverageWeightG());
                    ps.setString(27, session.getNotes());
                    ps.setBoolean(28, session.isDemo());
                    JdbcUtils.setNullableTimestamp(ps, 29, session.getCreatedAt());
                    JdbcUtils.setNullableTimestamp(ps, 30, session.getUpdatedAt());
                    ps.executeUpdate();
                }
                replaceEquipmentLinks(c, session.getId(), session.getEquipmentIds());
                for (BrewEvent ev : session.getEvents()) {
                    insertEventIfAbsent(c, ev);
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la sesión de preparación", e);
        }
    }

    public BrewSession startSession(String userId, String sessionId, Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null) {
            return null;
        }
        if (!BrewSession.STATUS_PREPARED.equals(session.getStatus())) {
            return session;
        }
        String sql = "UPDATE brew_sessions SET status = ?, started_at = ?, paused_at = NULL, "
                + "accumulated_pause_ms = 0, updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, BrewSession.STATUS_ACTIVE);
                JdbcUtils.setNullableTimestamp(ps, 2, now);
                JdbcUtils.setNullableTimestamp(ps, 3, now);
                ps.setString(4, sessionId);
                ps.setString(5, userId);
                ps.executeUpdate();
            }
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, null, null, BrewEvent.TYPE_STARTED,
                    now, 0L, session.getPlannedWaterG(), null,
                    safeIdem(idempotencyKey, "START"), "Inicio de cronómetro"
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Error iniciando sesión", e);
        }
        return findById(userId, sessionId);
    }

    public BrewSession pauseSession(String userId, String sessionId, Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null || !BrewSession.STATUS_ACTIVE.equals(session.getStatus())) {
            return session;
        }
        long elapsedMs = BrewTimerEngine.computeElapsedMs(session, now);
        String sql = "UPDATE brew_sessions SET status = ?, paused_at = ?, updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, BrewSession.STATUS_PAUSED);
                JdbcUtils.setNullableTimestamp(ps, 2, now);
                JdbcUtils.setNullableTimestamp(ps, 3, now);
                ps.setString(4, sessionId);
                ps.setString(5, userId);
                ps.executeUpdate();
            }
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, null, null, BrewEvent.TYPE_PAUSED,
                    now, elapsedMs, null, null,
                    safeIdem(idempotencyKey, "PAUSE-" + now.toEpochMilli()), "Sesión en pausa"
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Error pausando sesión", e);
        }
        return findById(userId, sessionId);
    }

    public BrewSession resumeSession(String userId, String sessionId, Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null || !BrewSession.STATUS_PAUSED.equals(session.getStatus())) {
            return session;
        }
        long newAccumPauseMs = BrewTimerEngine.computeAccumulatedPauseAfterResume(
                session.getAccumulatedPauseMs(), session.getPausedAt(), now);
        long elapsedMs = Math.max(0L, now.toEpochMilli() - session.getStartedAtEpochMs() - newAccumPauseMs);

        String sql = "UPDATE brew_sessions SET status = ?, paused_at = NULL, accumulated_pause_ms = ?, "
                + "updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, BrewSession.STATUS_ACTIVE);
                ps.setLong(2, newAccumPauseMs);
                JdbcUtils.setNullableTimestamp(ps, 3, now);
                ps.setString(4, sessionId);
                ps.setString(5, userId);
                ps.executeUpdate();
            }
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, null, null, BrewEvent.TYPE_RESUMED,
                    now, elapsedMs, null, null,
                    safeIdem(idempotencyKey, "RESUME-" + now.toEpochMilli()), "Sesión reanudada"
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Error reanudando sesión", e);
        }
        return findById(userId, sessionId);
    }

    /**
     * Registra la finalización u omisión confirmada de un paso con idempotencia por
     * {@code (sessionId, idempotencyKey)} y por orden de paso. Tolera registro tardío guardando
     * el {@code occurredAt} y {@code elapsedMs} reales.
     */
    public BrewSession recordStepEvent(String userId, String sessionId, RecipeStep step,
                                       boolean skipped, Double actualWaterG, String notes,
                                       Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null || step == null) {
            return session;
        }
        if (BrewTimerEngine.handledStepOrders(session.getEvents()).contains(Integer.valueOf(step.getOrder()))) {
            // Idempotencia: el paso ya fue registrado previamente (p. ej. doble clic)
            return session;
        }
        long elapsedMs = BrewTimerEngine.computeElapsedMs(session, now);
        Double targetWater = step.getCumulativeWaterG() != null ? step.getCumulativeWaterG() : step.getWaterDoseG();
        String eventType = skipped ? BrewEvent.TYPE_STEP_SKIPPED : BrewEvent.TYPE_STEP_COMPLETED;
        String idem = safeIdem(idempotencyKey, "STEP-" + step.getOrder());

        try (Connection c = dataSource.getConnection()) {
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, step.getId(), Integer.valueOf(step.getOrder()),
                    eventType, now, elapsedMs, targetWater, actualWaterG, idem, notes
            ));
            if (actualWaterG != null) {
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE brew_sessions SET actual_water_g = ?, updated_at = ? WHERE id = ? AND user_id = ?")) {
                    JdbcUtils.setNullableDouble(up, 1, actualWaterG);
                    JdbcUtils.setNullableTimestamp(up, 2, now);
                    up.setString(3, sessionId);
                    up.setString(4, userId);
                    up.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error registrando etapa de preparación", e);
        }
        return findById(userId, sessionId);
    }

    public BrewSession completeSession(String userId, String sessionId,
                                       Double actualDoseG, Double actualWaterG,
                                       Double actualTemperatureC, String actualGrindSetting,
                                       Integer overrideTotalTimeSeconds, Double finalBeverageWeightG,
                                       String notes, Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null) {
            return null;
        }
        if (BrewSession.STATUS_COMPLETED.equals(session.getStatus())) {
            return session;
        }
        long elapsedMs = BrewTimerEngine.computeElapsedMs(session, now);
        int computedTotalSeconds = (int) Math.round(elapsedMs / 1000.0);
        Integer finalTotalSeconds = overrideTotalTimeSeconds != null
                ? overrideTotalTimeSeconds
                : Integer.valueOf(computedTotalSeconds);

        Double resolvedDose = actualDoseG != null ? actualDoseG : session.getActualDoseG();
        Double resolvedWater = actualWaterG != null ? actualWaterG : session.getActualWaterG();
        Double resolvedTemp = actualTemperatureC != null ? actualTemperatureC : session.getActualTemperatureC();
        String resolvedGrind = actualGrindSetting != null ? actualGrindSetting : session.getActualGrindSetting();

        String sql = "UPDATE brew_sessions SET status = ?, paused_at = NULL, completed_at = ?, "
                + "actual_dose_g = ?, actual_water_g = ?, actual_temperature_c = ?, actual_grind_setting = ?, "
                + "total_time_seconds = ?, final_beverage_weight_g = ?, notes = ?, updated_at = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, BrewSession.STATUS_COMPLETED);
                JdbcUtils.setNullableTimestamp(ps, 2, now);
                JdbcUtils.setNullableDouble(ps, 3, resolvedDose);
                JdbcUtils.setNullableDouble(ps, 4, resolvedWater);
                JdbcUtils.setNullableDouble(ps, 5, resolvedTemp);
                ps.setString(6, resolvedGrind);
                JdbcUtils.setNullableInt(ps, 7, finalTotalSeconds);
                JdbcUtils.setNullableDouble(ps, 8, finalBeverageWeightG);
                ps.setString(9, notes != null ? notes : session.getNotes());
                JdbcUtils.setNullableTimestamp(ps, 10, now);
                ps.setString(11, sessionId);
                ps.setString(12, userId);
                ps.executeUpdate();
            }
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, null, null, BrewEvent.TYPE_COMPLETED,
                    now, elapsedMs, session.getPlannedWaterG(), resolvedWater,
                    safeIdem(idempotencyKey, "COMPLETE"), notes
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Error finalizando sesión", e);
        }
        return findById(userId, sessionId);
    }

    public BrewSession cancelSession(String userId, String sessionId, String reason,
                                     Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null || BrewSession.STATUS_CANCELLED.equals(session.getStatus())) {
            return session;
        }
        long elapsedMs = BrewTimerEngine.computeElapsedMs(session, now);
        String sql = "UPDATE brew_sessions SET status = ?, paused_at = NULL, completed_at = ?, "
                + "notes = ?, updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, BrewSession.STATUS_CANCELLED);
                JdbcUtils.setNullableTimestamp(ps, 2, now);
                ps.setString(3, reason != null ? reason : session.getNotes());
                JdbcUtils.setNullableTimestamp(ps, 4, now);
                ps.setString(5, sessionId);
                ps.setString(6, userId);
                ps.executeUpdate();
            }
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, null, null, BrewEvent.TYPE_CANCELLED,
                    now, elapsedMs, null, null, safeIdem(idempotencyKey, "CANCEL"), reason
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Error cancelando sesión", e);
        }
        return findById(userId, sessionId);
    }

    /**
     * Registra una corrección auditable sobre una sesión finalizada sin borrar el historial de lo ocurrido.
     */
    public BrewSession recordAuditableCorrection(String userId, String sessionId,
                                                 Double actualDoseG, Double actualWaterG,
                                                 Double actualTemperatureC, String actualGrindSetting,
                                                 Integer totalTimeSeconds, Double finalBeverageWeightG,
                                                 String reason, Instant now, String idempotencyKey) {
        BrewSession session = findById(userId, sessionId);
        if (session == null) {
            return null;
        }
        String sql = "UPDATE brew_sessions SET actual_dose_g = ?, actual_water_g = ?, actual_temperature_c = ?, "
                + "actual_grind_setting = ?, total_time_seconds = ?, final_beverage_weight_g = ?, updated_at = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                JdbcUtils.setNullableDouble(ps, 1, actualDoseG);
                JdbcUtils.setNullableDouble(ps, 2, actualWaterG);
                JdbcUtils.setNullableDouble(ps, 3, actualTemperatureC);
                ps.setString(4, actualGrindSetting);
                JdbcUtils.setNullableInt(ps, 5, totalTimeSeconds);
                JdbcUtils.setNullableDouble(ps, 6, finalBeverageWeightG);
                JdbcUtils.setNullableTimestamp(ps, 7, now);
                ps.setString(8, sessionId);
                ps.setString(9, userId);
                ps.executeUpdate();
            }
            insertEventIfAbsent(c, new BrewEvent(
                    Ids.newId(), sessionId, null, null, BrewEvent.TYPE_CORRECTION,
                    now, BrewTimerEngine.computeElapsedMs(session, now),
                    session.getActualWaterG(), actualWaterG,
                    safeIdem(idempotencyKey, "CORR-" + now.toEpochMilli()),
                    reason != null ? reason : "Corrección manual auditable"
            ));
        } catch (SQLException e) {
            throw new IllegalStateException("Error guardando corrección de sesión", e);
        }
        return findById(userId, sessionId);
    }

    public BrewSession findById(String userId, String sessionId) {
        if (sessionId == null || userId == null) {
            return null;
        }
        String sql = "SELECT * FROM brew_sessions WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                List<String> eqIds = loadEquipmentIds(c, sessionId);
                List<BrewEvent> events = loadEvents(c, sessionId);
                return mapSession(rs, eqIds, events);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando sesión de preparación", e);
        }
    }

    public BrewSession findLatestInProgress(String userId) {
        String sql = "SELECT * FROM brew_sessions WHERE user_id = ? "
                + "AND status IN ('ACTIVE', 'PAUSED', 'PREPARED') ORDER BY updated_at DESC";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String sId = rs.getString("id");
                return mapSession(rs, loadEquipmentIds(c, sId), loadEvents(c, sId));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error buscando sesión activa", e);
        }
    }

    public List<BrewSession> findAllByUser(String userId) {
        String sql = "SELECT * FROM brew_sessions WHERE user_id = ? ORDER BY created_at DESC";
        List<BrewSession> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sId = rs.getString("id");
                    list.add(mapSession(rs, loadEquipmentIds(c, sId), loadEvents(c, sId)));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando sesiones de preparación", e);
        }
    }

    private static void insertEventIfAbsent(Connection c, BrewEvent ev) throws SQLException {
        String check = "SELECT id FROM brew_events WHERE brew_session_id = ? AND idempotency_key = ?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, ev.getBrewSessionId());
            ps.setString(2, ev.getIdempotencyKey());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        String ins = "INSERT INTO brew_events (id, brew_session_id, recipe_step_id, step_order, "
                + "event_type, occurred_at, elapsed_ms, target_value, actual_value, idempotency_key, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(ins)) {
            ps.setString(1, ev.getId() != null ? ev.getId() : Ids.newId());
            ps.setString(2, ev.getBrewSessionId());
            ps.setString(3, ev.getRecipeStepId());
            JdbcUtils.setNullableInt(ps, 4, ev.getStepOrder());
            ps.setString(5, ev.getEventType());
            JdbcUtils.setNullableTimestamp(ps, 6, ev.getOccurredAt());
            ps.setLong(7, ev.getElapsedMs());
            JdbcUtils.setNullableDouble(ps, 8, ev.getTargetValue());
            JdbcUtils.setNullableDouble(ps, 9, ev.getActualValue());
            ps.setString(10, ev.getIdempotencyKey());
            ps.setString(11, ev.getNotes());
            ps.executeUpdate();
        }
    }

    private static void replaceEquipmentLinks(Connection c, String sessionId, List<String> eqIds)
            throws SQLException {
        try (PreparedStatement del = c.prepareStatement("DELETE FROM brew_session_equipment WHERE brew_session_id = ?")) {
            del.setString(1, sessionId);
            del.executeUpdate();
        }
        if (eqIds == null || eqIds.isEmpty()) {
            return;
        }
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO brew_session_equipment (brew_session_id, equipment_id) VALUES (?, ?)")) {
            for (String eqId : eqIds) {
                if (eqId != null && !eqId.trim().isEmpty()) {
                    ins.setString(1, sessionId);
                    ins.setString(2, eqId.trim());
                    ins.addBatch();
                }
            }
            ins.executeBatch();
        }
    }

    private static List<String> loadEquipmentIds(Connection c, String sessionId) throws SQLException {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT equipment_id FROM brew_session_equipment WHERE brew_session_id = ?")) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("equipment_id"));
                }
            }
        }
        return ids;
    }

    private static List<BrewEvent> loadEvents(Connection c, String sessionId) throws SQLException {
        List<BrewEvent> events = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM brew_events WHERE brew_session_id = ? ORDER BY occurred_at ASC, elapsed_ms ASC")) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(new BrewEvent(
                            rs.getString("id"),
                            rs.getString("brew_session_id"),
                            rs.getString("recipe_step_id"),
                            JdbcUtils.getNullableInt(rs, "step_order"),
                            rs.getString("event_type"),
                            JdbcUtils.getNullableInstant(rs, "occurred_at"),
                            rs.getLong("elapsed_ms"),
                            JdbcUtils.getNullableDouble(rs, "target_value"),
                            JdbcUtils.getNullableDouble(rs, "actual_value"),
                            rs.getString("idempotency_key"),
                            rs.getString("notes")
                    ));
                }
            }
        }
        return events;
    }

    private static BrewSession mapSession(ResultSet rs, List<String> eqIds, List<BrewEvent> events)
            throws SQLException {
        return new BrewSession(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("recipe_id"),
                rs.getString("coffee_lot_id"),
                rs.getString("roast_batch_id"),
                rs.getString("water_profile_id"),
                eqIds,
                rs.getString("status"),
                JdbcUtils.getNullableInstant(rs, "started_at"),
                JdbcUtils.getNullableInstant(rs, "paused_at"),
                rs.getLong("accumulated_pause_ms"),
                JdbcUtils.getNullableInstant(rs, "completed_at"),
                rs.getString("planned_recipe_name"),
                rs.getString("planned_method"),
                JdbcUtils.getNullableDouble(rs, "planned_dose_g"),
                JdbcUtils.getNullableDouble(rs, "planned_water_g"),
                JdbcUtils.getNullableDouble(rs, "planned_temperature_c"),
                rs.getString("planned_grind_setting"),
                JdbcUtils.getNullableInt(rs, "planned_target_time_min_s"),
                JdbcUtils.getNullableInt(rs, "planned_target_time_max_s"),
                rs.getString("recipe_snapshot_json"),
                JdbcUtils.getNullableDouble(rs, "actual_dose_g"),
                JdbcUtils.getNullableDouble(rs, "actual_water_g"),
                JdbcUtils.getNullableDouble(rs, "actual_temperature_c"),
                rs.getString("actual_grind_setting"),
                JdbcUtils.getNullableInt(rs, "total_time_seconds"),
                JdbcUtils.getNullableDouble(rs, "final_beverage_weight_g"),
                rs.getString("notes"),
                rs.getBoolean("is_demo"),
                events,
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }

    private static String safeIdem(String candidate, String fallback) {
        if (candidate != null && !candidate.trim().isEmpty()) {
            String t = candidate.trim();
            return t.length() > 90 ? t.substring(0, 90) : t;
        }
        return fallback;
    }
}
