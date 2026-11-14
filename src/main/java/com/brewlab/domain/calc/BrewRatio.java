package com.brewlab.domain.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Ratio de preparación expresado como 1:x, donde x = agua total (g) / dosis de café (g).
 * <p>
 * No es rendimiento de extracción: no usa TDS ni masa de bebida. Ausencia de datos y dosis
 * no positiva producen un resultado sin valor, nunca 0.
 */
public final class BrewRatio {

    public enum Status {
        /** Ratio calculado. */
        OK,
        /** Falta la dosis, el agua o ambos: no se puede calcular. */
        MISSING_DATA,
        /** Dosis igual o menor que cero: el cálculo se bloquea (división por cero). */
        INVALID_DOSE,
        /** Agua negativa: dato incoherente. */
        INVALID_WATER
    }

    /** Decimales usados para mostrar el ratio (1:16,7). */
    public static final int DISPLAY_DECIMALS = 1;
    public static final String FORMULA = "ratio 1:x = agua total (g) ÷ dosis de café (g)";

    private final Status status;
    private final Double value;

    private BrewRatio(Status status, Double value) {
        this.status = status;
        this.value = value;
    }

    public static BrewRatio of(Double waterG, Double doseG) {
        if (waterG == null || doseG == null) {
            return new BrewRatio(Status.MISSING_DATA, null);
        }
        if (doseG.isNaN() || doseG <= 0) {
            return new BrewRatio(Status.INVALID_DOSE, null);
        }
        if (waterG.isNaN() || waterG < 0) {
            return new BrewRatio(Status.INVALID_WATER, null);
        }
        return new BrewRatio(Status.OK, waterG / doseG);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isComputed() {
        return status == Status.OK;
    }

    /** Valor sin redondear (p. ej. 16,666…) o {@code null} si no se pudo calcular. */
    public Double getValue() {
        return value;
    }

    /** Valor redondeado (HALF_UP) a {@link #DISPLAY_DECIMALS} decimales, o {@code null}. */
    public BigDecimal getRounded() {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(DISPLAY_DECIMALS, RoundingMode.HALF_UP);
    }

    /** Texto para la interfaz en español, p. ej. "1:16,7"; {@code null} si no hay valor. */
    public String format(Locale locale) {
        BigDecimal rounded = getRounded();
        if (rounded == null) {
            return null;
        }
        DecimalFormat df = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(locale));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return "1:" + df.format(rounded);
    }
}
