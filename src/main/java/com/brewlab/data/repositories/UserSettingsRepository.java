package com.brewlab.data.repositories;

import com.brewlab.domain.model.UserSettings;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/** Repositorio JDBC de preferencias del usuario ({@code user_settings}). */
public final class UserSettingsRepository {
    private final DataSource dataSource;

    public UserSettingsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public UserSettings findOrDefault(String userId) {
        String sql = "SELECT user_id, sound_enabled, keep_screen_awake, timer_theme, updated_at "
                + "FROM user_settings WHERE user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return UserSettings.defaults(userId);
                }
                Timestamp ts = rs.getTimestamp("updated_at");
                UserSettings.TimerTheme theme = UserSettings.TimerTheme.fromCode(rs.getString("timer_theme"));
                return new UserSettings(
                        rs.getString("user_id"),
                        rs.getBoolean("sound_enabled"),
                        rs.getBoolean("keep_screen_awake"),
                        theme != null ? theme : UserSettings.TimerTheme.AUTO,
                        ts != null ? ts.toInstant() : null
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando preferencias de usuario", e);
        }
    }

    public UserSettings save(UserSettings settings, Instant now) {
        String updateSql = "UPDATE user_settings SET sound_enabled = ?, keep_screen_awake = ?, "
                + "timer_theme = ?, updated_at = ? WHERE user_id = ?";
        String insertSql = "INSERT INTO user_settings (user_id, sound_enabled, keep_screen_awake, timer_theme, updated_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        Timestamp ts = Timestamp.from(now);
        try (Connection c = dataSource.getConnection()) {
            try (PreparedStatement up = c.prepareStatement(updateSql)) {
                up.setBoolean(1, settings.isSoundEnabled());
                up.setBoolean(2, settings.isKeepScreenAwake());
                up.setString(3, settings.getTimerThemeCode());
                up.setTimestamp(4, ts);
                up.setString(5, settings.getUserId());
                if (up.executeUpdate() == 0) {
                    try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                        ins.setString(1, settings.getUserId());
                        ins.setBoolean(2, settings.isSoundEnabled());
                        ins.setBoolean(3, settings.isKeepScreenAwake());
                        ins.setString(4, settings.getTimerThemeCode());
                        ins.setTimestamp(5, ts);
                        ins.executeUpdate();
                    }
                }
            }
            return new UserSettings(
                    settings.getUserId(),
                    settings.isSoundEnabled(),
                    settings.isKeepScreenAwake(),
                    settings.getTimerTheme(),
                    now
            );
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron guardar las preferencias", e);
        }
    }
}
