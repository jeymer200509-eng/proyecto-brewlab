package com.brewlab.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Protección CSRF por doble envío (cookie + campo oculto _csrf). */
public final class CsrfTokens {
    public static final String COOKIE_NAME = "brewlab_csrf";
    public static final String FORM_FIELD = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokens() {
    }

    public static String generate() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean matches(String cookieToken, String formToken) {
        if (cookieToken == null || formToken == null
                || cookieToken.length() < 16 || formToken.length() < 16) {
            return false;
        }
        return MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8),
                formToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
