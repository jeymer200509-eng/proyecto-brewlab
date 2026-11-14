package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.model.WaterProfile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Repositorio JDBC para {@link WaterProfile} ({@code water_profiles}). */
public final class WaterProfileRepository {
    private final DataSource dataSource;

    public WaterProfileRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(WaterProfile wp) {
        String sql = "INSERT INTO water_profiles (id, user_id, name, source, tds_ppm, hardness_gh, alkalinity_kh, "
                + "minerals, measured_at, notes, is_demo, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, wp.getId());
            ps.setString(2, wp.getUserId());
            ps.setString(3, wp.getName());
            ps.setString(4, wp.getSource());
            JdbcUtils.setNullableDouble(ps, 5, wp.getTdsPpm());
            JdbcUtils.setNullableDouble(ps, 6, wp.getHardnessGh());
            JdbcUtils.setNullableDouble(ps, 7, wp.getAlkalinityKh());
            ps.setString(8, wp.getMinerals());
            JdbcUtils.setNullableDate(ps, 9, wp.getMeasuredAt());
            ps.setString(10, wp.getNotes());
            ps.setBoolean(11, wp.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 12, wp.getCreatedAt());
            JdbcUtils.setNullableTimestamp(ps, 13, wp.getUpdatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el perfil de agua", e);
        }
    }

    public void update(WaterProfile wp) {
        String sql = "UPDATE water_profiles SET name = ?, source = ?, tds_ppm = ?, hardness_gh = ?, "
                + "alkalinity_kh = ?, minerals = ?, measured_at = ?, notes = ?, is_demo = ?, updated_at = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, wp.getName());
            ps.setString(2, wp.getSource());
            JdbcUtils.setNullableDouble(ps, 3, wp.getTdsPpm());
            JdbcUtils.setNullableDouble(ps, 4, wp.getHardnessGh());
            JdbcUtils.setNullableDouble(ps, 5, wp.getAlkalinityKh());
            ps.setString(6, wp.getMinerals());
            JdbcUtils.setNullableDate(ps, 7, wp.getMeasuredAt());
            ps.setString(8, wp.getNotes());
            ps.setBoolean(9, wp.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 10, wp.getUpdatedAt());
            ps.setString(11, wp.getId());
            ps.setString(12, wp.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el perfil de agua", e);
        }
    }

    public void delete(String userId, String id) {
        String sql = "DELETE FROM water_profiles WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el perfil de agua", e);
        }
    }

    public WaterProfile findById(String userId, String id) {
        if (id == null || userId == null) {
            return null;
        }
        String sql = "SELECT * FROM water_profiles WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando perfil de agua", e);
        }
    }

    public List<WaterProfile> findAllByUser(String userId) {
        String sql = "SELECT * FROM water_profiles WHERE user_id = ? ORDER BY name ASC";
        List<WaterProfile> list = new ArrayList<>();
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
            throw new IllegalStateException("Error listando perfiles de agua", e);
        }
    }

    private static WaterProfile mapRow(ResultSet rs) throws SQLException {
        return new WaterProfile(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getString("source"),
                JdbcUtils.getNullableDouble(rs, "tds_ppm"),
                JdbcUtils.getNullableDouble(rs, "hardness_gh"),
                JdbcUtils.getNullableDouble(rs, "alkalinity_kh"),
                rs.getString("minerals"),
                JdbcUtils.getNullableDate(rs, "measured_at"),
                rs.getString("notes"),
                rs.getBoolean("is_demo"),
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }
}
