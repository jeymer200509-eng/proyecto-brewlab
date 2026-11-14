package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.model.RoastBatch;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Repositorio JDBC para {@link RoastBatch} ({@code roast_batches}). */
public final class RoastBatchRepository {
    private final DataSource dataSource;

    public RoastBatchRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(RoastBatch batch) {
        String sql = "INSERT INTO roast_batches (id, user_id, coffee_lot_id, batch_code, roast_date, "
                + "roast_level, roaster, machine, roast_notes, weight_loss_pct, agtron, opened_on, "
                + "preferred_by_date, available_weight_g, is_demo, archived_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, batch.getId());
            ps.setString(2, batch.getUserId());
            ps.setString(3, batch.getCoffeeLotId());
            ps.setString(4, batch.getBatchCode());
            JdbcUtils.setNullableDate(ps, 5, batch.getRoastDate());
            ps.setString(6, batch.getRoastLevel());
            ps.setString(7, batch.getRoaster());
            ps.setString(8, batch.getMachine());
            ps.setString(9, batch.getRoastNotes());
            JdbcUtils.setNullableDouble(ps, 10, batch.getWeightLossPct());
            JdbcUtils.setNullableDouble(ps, 11, batch.getAgtron());
            JdbcUtils.setNullableDate(ps, 12, batch.getOpenedOn());
            JdbcUtils.setNullableDate(ps, 13, batch.getPreferredByDate());
            JdbcUtils.setNullableDouble(ps, 14, batch.getAvailableWeightG());
            ps.setBoolean(15, batch.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 16, batch.getArchivedAt());
            JdbcUtils.setNullableTimestamp(ps, 17, batch.getCreatedAt());
            JdbcUtils.setNullableTimestamp(ps, 18, batch.getUpdatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el lote de tostión", e);
        }
    }

    public void update(RoastBatch batch) {
        String sql = "UPDATE roast_batches SET batch_code = ?, roast_date = ?, roast_level = ?, "
                + "roaster = ?, machine = ?, roast_notes = ?, weight_loss_pct = ?, agtron = ?, "
                + "opened_on = ?, preferred_by_date = ?, available_weight_g = ?, is_demo = ?, "
                + "archived_at = ?, updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, batch.getBatchCode());
            JdbcUtils.setNullableDate(ps, 2, batch.getRoastDate());
            ps.setString(3, batch.getRoastLevel());
            ps.setString(4, batch.getRoaster());
            ps.setString(5, batch.getMachine());
            ps.setString(6, batch.getRoastNotes());
            JdbcUtils.setNullableDouble(ps, 7, batch.getWeightLossPct());
            JdbcUtils.setNullableDouble(ps, 8, batch.getAgtron());
            JdbcUtils.setNullableDate(ps, 9, batch.getOpenedOn());
            JdbcUtils.setNullableDate(ps, 10, batch.getPreferredByDate());
            JdbcUtils.setNullableDouble(ps, 11, batch.getAvailableWeightG());
            ps.setBoolean(12, batch.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 13, batch.getArchivedAt());
            JdbcUtils.setNullableTimestamp(ps, 14, batch.getUpdatedAt());
            ps.setString(15, batch.getId());
            ps.setString(16, batch.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el lote de tostión", e);
        }
    }

    public RoastBatch findById(String userId, String id) {
        if (id == null || userId == null) {
            return null;
        }
        String sql = "SELECT * FROM roast_batches WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando lote de tostión", e);
        }
    }

    public List<RoastBatch> findByCoffeeLot(String userId, String coffeeLotId, boolean includeArchived) {
        String sql = "SELECT * FROM roast_batches WHERE user_id = ? AND coffee_lot_id = ? "
                + "ORDER BY roast_date DESC, created_at DESC";
        List<RoastBatch> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, coffeeLotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoastBatch b = mapRow(rs);
                    if (!includeArchived && b.isArchived()) {
                        continue;
                    }
                    list.add(b);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando lotes de tostión del café", e);
        }
    }

    public List<RoastBatch> findAllByUser(String userId, boolean includeArchived) {
        String sql = "SELECT * FROM roast_batches WHERE user_id = ? ORDER BY roast_date DESC, created_at DESC";
        List<RoastBatch> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoastBatch b = mapRow(rs);
                    if (!includeArchived && b.isArchived()) {
                        continue;
                    }
                    list.add(b);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando lotes de tostión", e);
        }
    }

    private static RoastBatch mapRow(ResultSet rs) throws SQLException {
        return new RoastBatch(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("coffee_lot_id"),
                rs.getString("batch_code"),
                JdbcUtils.getNullableDate(rs, "roast_date"),
                rs.getString("roast_level"),
                rs.getString("roaster"),
                rs.getString("machine"),
                rs.getString("roast_notes"),
                JdbcUtils.getNullableDouble(rs, "weight_loss_pct"),
                JdbcUtils.getNullableDouble(rs, "agtron"),
                JdbcUtils.getNullableDate(rs, "opened_on"),
                JdbcUtils.getNullableDate(rs, "preferred_by_date"),
                JdbcUtils.getNullableDouble(rs, "available_weight_g"),
                rs.getBoolean("is_demo"),
                JdbcUtils.getNullableInstant(rs, "archived_at"),
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }
}
