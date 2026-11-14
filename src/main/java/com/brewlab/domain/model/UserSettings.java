package com.brewlab.domain.model;

import java.time.Instant;

/** Preferencias de la persona usuaria (no son datos del café ni mediciones). */
public final class UserSettings {

    /** Tema del cronómetro del Brew Assistant. */
    public enum TimerTheme {
        AUTO("auto"), LIGHT("claro"), DARK("oscuro");

        private final String code;

        TimerTheme(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static TimerTheme fromCode(String code) {
            for (TimerTheme t : values()) {
                if (t.code.equals(code)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final String userId;
    private final boolean soundEnabled;
    private final boolean keepScreenAwake;
    private final TimerTheme timerTheme;
    private final Instant updatedAt;

    public UserSettings(String userId, boolean soundEnabled, boolean keepScreenAwake, TimerTheme timerTheme,
                        Instant updatedAt) {
        this.userId = userId;
        this.soundEnabled = soundEnabled;
        this.keepScreenAwake = keepScreenAwake;
        this.timerTheme = timerTheme == null ? TimerTheme.AUTO : timerTheme;
        this.updatedAt = updatedAt;
    }

    /** Valores por defecto: sin sonido (no sorprender), sin bloqueo de pantalla, tema automático. */
    public static UserSettings defaults(String userId) {
        return new UserSettings(userId, false, false, TimerTheme.AUTO, null);
    }

    public String getUserId() {
        return userId;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isKeepScreenAwake() {
        return keepScreenAwake;
    }

    public TimerTheme getTimerTheme() {
        return timerTheme;
    }

    public String getTimerThemeCode() {
        return timerTheme.getCode();
    }

    public String getDefaultSensoryScale() {
        return "1-5";
    }

    /** {@code null} si nunca se guardaron (se usan los valores por defecto). */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
