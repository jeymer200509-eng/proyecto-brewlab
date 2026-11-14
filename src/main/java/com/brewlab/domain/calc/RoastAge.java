package com.brewlab.domain.calc;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Días desde la tostión: diferencia en días calendario entre la fecha local actual y la fecha de
 * tostión del lote seleccionado. Nunca se almacena; se calcula cada vez.
 * <p>
 * No define ventanas de frescura ni juzga si un café es mejor o peor por su edad.
 */
public final class RoastAge {

    public enum Status {
        /** Edad calculada (0 = tostado hoy). */
        OK,
        /** Fecha de tostión posterior a hoy: se muestra advertencia. */
        FUTURE_DATE,
        /** Sin fecha de tostión. */
        UNKNOWN
    }

    private final Status status;
    private final Long days;
    private final LocalDate roastDate;
    private final LocalDate referenceDate;

    private RoastAge(Status status, Long days, LocalDate roastDate, LocalDate referenceDate) {
        this.status = status;
        this.days = days;
        this.roastDate = roastDate;
        this.referenceDate = referenceDate;
    }

    /**
     * @param roastDate fecha calendario de tostión (puede ser {@code null})
     * @param today     fecha local actual del usuario
     */
    public static RoastAge between(LocalDate roastDate, LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("today es obligatorio");
        }
        if (roastDate == null) {
            return new RoastAge(Status.UNKNOWN, null, null, today);
        }
        long diff = ChronoUnit.DAYS.between(roastDate, today);
        if (diff < 0) {
            return new RoastAge(Status.FUTURE_DATE, diff, roastDate, today);
        }
        return new RoastAge(Status.OK, diff, roastDate, today);
    }

    public Status getStatus() {
        return status;
    }

    /** Días transcurridos; negativo si la fecha es futura; {@code null} si se desconoce. */
    public Long getDays() {
        return days;
    }

    public boolean isFuture() {
        return status == Status.FUTURE_DATE;
    }

    public LocalDate getRoastDate() {
        return roastDate;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }
}
