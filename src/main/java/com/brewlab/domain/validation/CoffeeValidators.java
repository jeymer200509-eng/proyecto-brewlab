package com.brewlab.domain.validation;

import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.RoastBatch;

/** Validadores de dominio puros para {@link CoffeeLot} y {@link RoastBatch}. */
public final class CoffeeValidators {

    private CoffeeValidators() {
    }

    public static FieldErrors validateCoffeeLot(CoffeeLot lot) {
        FieldErrors errors = new FieldErrors();
        if (lot.getCode() == null || lot.getCode().trim().isEmpty()) {
            errors.add("code", "El código del lote es obligatorio.");
        }
        if (lot.getName() == null || lot.getName().trim().isEmpty()) {
            errors.add("name", "El nombre o denominación del café es obligatorio.");
        }
        if (lot.getAltitudeMin() != null && lot.getAltitudeMin() < 0) {
            errors.add("altitudeMin", "La altitud mínima no puede ser negativa.");
        }
        if (lot.getAltitudeMax() != null && lot.getAltitudeMax() < 0) {
            errors.add("altitudeMax", "La altitud máxima no puede ser negativa.");
        }
        if (lot.getAltitudeMin() != null && lot.getAltitudeMax() != null
                && lot.getAltitudeMin() > lot.getAltitudeMax()) {
            errors.add("altitudeMax", "La altitud máxima debe ser mayor o igual a la mínima.");
        }
        if (lot.getMoisturePct() != null && (lot.getMoisturePct() <= 0 || lot.getMoisturePct() > 40)) {
            errors.add("moisturePct", "La humedad debe estar entre 0 % y 40 %.");
        }
        if (lot.getDensity() != null && lot.getDensity() <= 0) {
            errors.add("density", "La densidad debe ser un valor positivo.");
        }
        return errors;
    }

    public static FieldErrors validateRoastBatch(RoastBatch batch) {
        FieldErrors errors = new FieldErrors();
        if (batch.getCoffeeLotId() == null || batch.getCoffeeLotId().trim().isEmpty()) {
            errors.add("coffeeLotId", "Debe estar vinculado a un lote de café.");
        }
        if (batch.getBatchCode() == null || batch.getBatchCode().trim().isEmpty()) {
            errors.add("batchCode", "El código de tostión es obligatorio.");
        }
        if (batch.getRoastDate() == null) {
            errors.add("roastDate", "La fecha de tostión es obligatoria.");
        }
        if (batch.getWeightLossPct() != null && (batch.getWeightLossPct() < 0 || batch.getWeightLossPct() >= 100)) {
            errors.add("weightLossPct", "La pérdida de masa (%) debe estar entre 0 y 100.");
        }
        if (batch.getAgtron() != null && batch.getAgtron() <= 0) {
            errors.add("agtron", "El valor Agtron debe ser positivo.");
        }
        if (batch.getAvailableWeightG() != null && batch.getAvailableWeightG() < 0) {
            errors.add("availableWeightG", "El peso disponible no puede ser negativo.");
        }
        return errors;
    }
}
