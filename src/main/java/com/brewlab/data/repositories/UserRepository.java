package com.brewlab.data.repositories;

import com.brewlab.domain.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;

/** Repositorio JDBC de cuentas de usuario ({@code users}). */
public final class UserRepository {
    private final DataSource dataSource;

    public UserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User create(String id, String email, String displayName, String passwordHash, Instant now) {
        String normalizedEmail = normalizeEmail(email);
        String sql = "INSERT INTO users (id, email, display_name, password_hash, token_version, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, 1, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            Timestamp ts = Timestamp.from(now);
            ps.setString(1, id);
            ps.setString(2, normalizedEmail);
            ps.setString(3, displayName.trim());
            ps.setString(4, passwordHash);
            ps.setTimestamp(5, ts);
            ps.setTimestamp(6, ts);
            ps.executeUpdate();
            return new User(id, normalizedEmail, displayName.trim(), passwordHash, 1, now, now, null);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear usuario", e);
        }
    }

    public User findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, email, display_name, password_hash, token_version, created_at, updated_at, last_login_at "
                + "FROM users WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando usuario por ID", e);
        }
    }

    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, email, display_name, password_hash, token_version, created_at, updated_at, last_login_at "
                + "FROM users WHERE email = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(email));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando usuario por correo", e);
        }
    }

    public void updateLastLogin(String userId, Instant when) {
        String sql = "UPDATE users SET last_login_at = ?, updated_at = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            Timestamp ts = Timestamp.from(when);
            ps.setTimestamp(1, ts);
            ps.setTimestamp(2, ts);
            ps.setString(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error actualizando último acceso", e);
        }
    }

    /** Revoca todos los tokens JWT emitidos previamente para el usuario incrementando su versión. */
    public void incrementTokenVersion(String userId, Instant when) {
        String sql = "UPDATE users SET token_version = token_version + 1, updated_at = ? WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(when));
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error revocando versión de token", e);
        }
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        Timestamp loginTs = rs.getTimestamp("last_login_at");
        return new User(
                rs.getString("id"),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("password_hash"),
                rs.getInt("token_version"),
                createdTs != null ? createdTs.toInstant() : Instant.EPOCH,
                updatedTs != null ? updatedTs.toInstant() : Instant.EPOCH,
                loginTs != null ? loginTs.toInstant() : null
        );
    }
}
