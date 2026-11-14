package com.brewlab.domain.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato de valores para la interfaz. Regla central: un valor desconocido ({@code null}) se
 * muestra como {@link #UNKNOWN}, nunca como 0.
 */
public final class Formats {

    /** Marcador visual de dato ausente. Las vistas añaden texto accesible "Sin dato". */
    public static final String UNKNOWN = "—";
    public static final Locale LOCALE = new Locale("es", "CO");

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", LOCALE);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE);

    private Formats() {
    }

    /** Número con los decimales indicados (coma decimal) o "—". Elimina ceros finales. */
    public static String number(Number value, int maxDecimals) {
        if (value == null) {
            return UNKNOWN;
        }
        double d = value.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return UNKNOWN;
        }
        StringBuilder pattern = new StringBuilder("#,##0");
        if (maxDecimals > 0) {
            pattern.append('.');
            for (int i = 0; i < maxDecimals; i++) {
                pattern.append('#');
            }
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(LOCALE));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(BigDecimal.valueOf(d));
    }

    /** Valor con unidad, p. ej. "15 g", "93 °C"; "—" si es desconocido. */
    public static String withUnit(Number value, int maxDecimals, String unit) {
        if (value == null) {
            return UNKNOWN;
        }
        String n = number(value, maxDecimals);
        return UNKNOWN.equals(n) ? UNKNOWN : n + "\u00a0" + unit;
    }

    /** Segundos como m:ss (p. ej. 165 → "2:45"); "—" si es desconocido. */
    public static String duration(Number seconds) {
        if (seconds == null) {
            return UNKNOWN;
        }
        long total = Math.round(seconds.doubleValue());
        boolean negative = total < 0;
        total = Math.abs(total);
        String s = (total / 60) + ":" + String.format(Locale.ROOT, "%02d", total % 60);
        return negative ? "−" + s : s;
    }

    public static String dateTime(Instant instant, ZoneId zone) {
        return instant == null ? UNKNOWN : DATE_TIME.format(instant.atZone(zone));
    }

    public static String date(LocalDate date) {
        return date == null ? UNKNOWN : DATE.format(date);
    }

    /** Texto: devuelve "—" si es nulo o vacío. */
    public static String text(String value) {
        return value == null || value.trim().isEmpty() ? UNKNOWN : value;
    }
}
