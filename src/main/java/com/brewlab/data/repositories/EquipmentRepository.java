package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.model.Equipment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Repositorio JDBC para {@link Equipment} ({@code equipment}). */
public final class EquipmentRepository {
    private final DataSource dataSource;

    public EquipmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(Equipment eq) {
        String sql = "INSERT INTO equipment (id, user_id, type, brand, model, name, settings_scale, notes, is_demo, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, eq.getId());
            ps.setString(2, eq.getUserId());
            ps.setString(3, eq.getType());
            ps.setString(4, eq.getBrand());
            ps.setString(5, eq.getModel());
            ps.setString(6, eq.getName());
            ps.setString(7, eq.getSettingsScale());
            ps.setString(8, eq.getNotes());
            ps.setBoolean(9, eq.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 10, eq.getCreatedAt());
            JdbcUtils.setNullableTimestamp(ps, 11, eq.getUpdatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el equipo", e);
        }
    }

    public void update(Equipment eq) {
        String sql = "UPDATE equipment SET type = ?, brand = ?, model = ?, name = ?, settings_scale = ?, "
                + "notes = ?, is_demo = ?, updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, eq.getType());
            ps.setString(2, eq.getBrand());
            ps.setString(3, eq.getModel());
            ps.setString(4, eq.getName());
            ps.setString(5, eq.getSettingsScale());
            ps.setString(6, eq.getNotes());
            ps.setBoolean(7, eq.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 8, eq.getUpdatedAt());
            ps.setString(9, eq.getId());
            ps.setString(10, eq.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el equipo", e);
        }
    }

    public void delete(String userId, String id) {
        String sql = "DELETE FROM equipment WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el equipo", e);
        }
    }

    public Equipment findById(String userId, String id) {
        if (id == null || userId == null) {
            return null;
        }
        String sql = "SELECT * FROM equipment WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando equipo", e);
        }
    }

    public List<Equipment> findAllByUser(String userId) {
        String sql = "SELECT * FROM equipment WHERE user_id = ? ORDER BY type ASC, name ASC";
        List<Equipment> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando equipos", e);
        }
    }

    private static Equipment mapRow(ResultSet rs) throws SQLException {
        return new Equipment(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("type"),
                rs.getString("brand"),
                rs.getString("model"),
                rs.getString("name"),
                rs.getString("settings_scale"),
                rs.getString("notes"),
                rs.getBoolean("is_demo"),
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }
}
