package com.brewlab.security;

import com.brewlab.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Servicio de emisión y verificación de JSON Web Tokens (JWT) firmados con HMAC-SHA256 (HS256).
 * El token viaja en cookie {@code HttpOnly; SameSite=Strict} para evitar exposición en JavaScript.
 */
public final class JwtService {
    public static final String COOKIE_NAME = "brewlab_jwt";

    public static final class VerifiedToken {
        private final String userId;
        private final String email;
        private final String displayName;
        private final int tokenVersion;
        private final Instant issuedAt;
        private final Instant expiresAt;

        public VerifiedToken(String userId, String email, String displayName,
                             int tokenVersion, Instant issuedAt, Instant expiresAt) {
            this.userId = userId;
            this.email = email;
            this.displayName = displayName;
            this.tokenVersion = tokenVersion;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getTokenVersion() {
            return tokenVersion;
        }

        public Instant getIssuedAt() {
            return issuedAt;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(String base64Secret, int ttlMinutes) {
        byte[] raw = decodeSecret(base64Secret);
        if (raw.length < 32) {
            throw new IllegalArgumentException("El secreto JWT debe tener al menos 256 bits (32 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.ttlSeconds = Math.max(60L, ttlMinutes * 60L);
    }

    public String issueToken(User user, Instant now) {
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("tv", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Verifica firma, vigencia y estructura usando el reloj actual del sistema. */
    public VerifiedToken verify(String token) {
        return verify(token, Instant.now());
    }

    /** Verifica firma, vigencia respecto a {@code referenceNow} y estructura. */
    public VerifiedToken verify(String token, final Instant referenceNow) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(new io.jsonwebtoken.Clock() {
                        @Override
                        public Date now() {
                            return Date.from(referenceNow);
                        }
                    })
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            String sub = claims.getSubject();
            String email = claims.get("email", String.class);
            String name = claims.get("name", String.class);
            Number tvNum = claims.get("tv", Number.class);
            Date iat = claims.getIssuedAt();
            Date exp = claims.getExpiration();
            if (sub == null || email == null || tvNum == null || exp == null) {
                return null;
            }
            return new VerifiedToken(
                    sub,
                    email,
                    name != null ? name : email,
                    tvNum.intValue(),
                    iat != null ? iat.toInstant() : Instant.EPOCH,
                    exp.toInstant()
            );
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public int getMaxAgeSeconds() {
        return (int) ttlSeconds;
    }

    private static byte[] decodeSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Secreto JWT requerido");
        }
        try {
            return Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            return secret.trim().getBytes(StandardCharsets.UTF_8);
        }
    }
}
