package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.model.CoffeeLot;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Repositorio JDBC para {@link CoffeeLot} ({@code coffee_lots}). */
public final class CoffeeLotRepository {
    private final DataSource dataSource;

    public CoffeeLotRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(CoffeeLot lot) {
        String sql = "INSERT INTO coffee_lots (id, user_id, code, name, country, region, municipality, "
                + "farm, producer, species, variety, process, altitude_min, altitude_max, harvest, "
                + "received_on, moisture_pct, density, screen_size, notes, is_demo, archived_at, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindLot(ps, lot);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el lote de café", e);
        }
    }

    public void update(CoffeeLot lot) {
        String sql = "UPDATE coffee_lots SET code = ?, name = ?, country = ?, region = ?, municipality = ?, "
                + "farm = ?, producer = ?, species = ?, variety = ?, process = ?, altitude_min = ?, altitude_max = ?, "
                + "harvest = ?, received_on = ?, moisture_pct = ?, density = ?, screen_size = ?, notes = ?, "
                + "is_demo = ?, archived_at = ?, updated_at = ? WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, lot.getCode());
            ps.setString(2, lot.getName());
            ps.setString(3, lot.getCountry());
            ps.setString(4, lot.getRegion());
            ps.setString(5, lot.getMunicipality());
            ps.setString(6, lot.getFarm());
            ps.setString(7, lot.getProducer());
            ps.setString(8, lot.getSpecies());
            ps.setString(9, lot.getVariety());
            ps.setString(10, lot.getProcess());
            JdbcUtils.setNullableInt(ps, 11, lot.getAltitudeMin());
            JdbcUtils.setNullableInt(ps, 12, lot.getAltitudeMax());
            ps.setString(13, lot.getHarvest());
            JdbcUtils.setNullableDate(ps, 14, lot.getReceivedOn());
            JdbcUtils.setNullableDouble(ps, 15, lot.getMoisturePct());
            JdbcUtils.setNullableDouble(ps, 16, lot.getDensity());
            ps.setString(17, lot.getScreenSize());
            ps.setString(18, lot.getNotes());
            ps.setBoolean(19, lot.isDemo());
            JdbcUtils.setNullableTimestamp(ps, 20, lot.getArchivedAt());
            JdbcUtils.setNullableTimestamp(ps, 21, lot.getUpdatedAt());
            ps.setString(22, lot.getId());
            ps.setString(23, lot.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el lote de café", e);
        }
    }

    public CoffeeLot findById(String userId, String id) {
        if (id == null || userId == null) {
            return null;
        }
        String sql = "SELECT * FROM coffee_lots WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando lote de café", e);
        }
    }

    public List<CoffeeLot> findAll(String userId, String query, boolean includeArchived) {
        String sql = "SELECT * FROM coffee_lots WHERE user_id = ? ORDER BY created_at DESC";
        List<CoffeeLot> list = new ArrayList<>();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CoffeeLot lot = mapRow(rs);
                    if (!includeArchived && lot.isArchived()) {
                        continue;
                    }
                    if (!q.isEmpty() && !matches(lot, q)) {
                        continue;
                    }
                    list.add(lot);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando lotes de café", e);
        }
    }

    private static boolean matches(CoffeeLot lot, String q) {
        return contains(lot.getCode(), q)
                || contains(lot.getName(), q)
                || contains(lot.getCountry(), q)
                || contains(lot.getRegion(), q)
                || contains(lot.getMunicipality(), q)
                || contains(lot.getFarm(), q)
                || contains(lot.getProducer(), q)
                || contains(lot.getVariety(), q)
                || contains(lot.getProcess(), q);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(q);
    }

    private static void bindLot(PreparedStatement ps, CoffeeLot lot) throws SQLException {
        ps.setString(1, lot.getId());
        ps.setString(2, lot.getUserId());
        ps.setString(3, lot.getCode());
        ps.setString(4, lot.getName());
        ps.setString(5, lot.getCountry());
        ps.setString(6, lot.getRegion());
        ps.setString(7, lot.getMunicipality());
        ps.setString(8, lot.getFarm());
        ps.setString(9, lot.getProducer());
        ps.setString(10, lot.getSpecies());
        ps.setString(11, lot.getVariety());
        ps.setString(12, lot.getProcess());
        JdbcUtils.setNullableInt(ps, 13, lot.getAltitudeMin());
        JdbcUtils.setNullableInt(ps, 14, lot.getAltitudeMax());
        ps.setString(15, lot.getHarvest());
        JdbcUtils.setNullableDate(ps, 16, lot.getReceivedOn());
        JdbcUtils.setNullableDouble(ps, 17, lot.getMoisturePct());
        JdbcUtils.setNullableDouble(ps, 18, lot.getDensity());
        ps.setString(19, lot.getScreenSize());
        ps.setString(20, lot.getNotes());
        ps.setBoolean(21, lot.isDemo());
        JdbcUtils.setNullableTimestamp(ps, 22, lot.getArchivedAt());
        JdbcUtils.setNullableTimestamp(ps, 23, lot.getCreatedAt());
        JdbcUtils.setNullableTimestamp(ps, 24, lot.getUpdatedAt());
    }

    private static CoffeeLot mapRow(ResultSet rs) throws SQLException {
        return new CoffeeLot(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("country"),
                rs.getString("region"),
                rs.getString("municipality"),
                rs.getString("farm"),
                rs.getString("producer"),
                rs.getString("species"),
                rs.getString("variety"),
                rs.getString("process"),
                JdbcUtils.getNullableInt(rs, "altitude_min"),
                JdbcUtils.getNullableInt(rs, "altitude_max"),
                rs.getString("harvest"),
                JdbcUtils.getNullableDate(rs, "received_on"),
                JdbcUtils.getNullableDouble(rs, "moisture_pct"),
                JdbcUtils.getNullableDouble(rs, "density"),
                rs.getString("screen_size"),
                rs.getString("notes"),
                rs.getBoolean("is_demo"),
                JdbcUtils.getNullableInstant(rs, "archived_at"),
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }
}
