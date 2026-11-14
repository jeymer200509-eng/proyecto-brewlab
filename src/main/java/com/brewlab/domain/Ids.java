package com.brewlab.domain;

import java.util.UUID;
import java.util.regex.Pattern;

/** Identificadores estables (UUID v4 en texto, 36 caracteres). */
public final class Ids {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private Ids() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** Valida formato antes de usar un id recibido del navegador. */
    public static boolean isValid(String candidate) {
        return candidate != null && UUID_PATTERN.matcher(candidate).matches();
    }
}
