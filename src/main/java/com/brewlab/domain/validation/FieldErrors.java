package com.brewlab.domain.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Errores de validación por campo. Guarda códigos de mensaje (p. ej. "email.invalid"), no
 * textos, para que la interfaz pueda traducirlos en el futuro.
 */
public final class FieldErrors {
    private final Map<String, String> errors = new LinkedHashMap<>();

    public FieldErrors add(String field, String messageCode) {
        if (!errors.containsKey(field)) {
            errors.put(field, messageCode);
        }
        return this;
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public boolean has(String field) {
        return errors.containsKey(field);
    }

    public String codeFor(String field) {
        return errors.get(field);
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(errors);
    }
}
