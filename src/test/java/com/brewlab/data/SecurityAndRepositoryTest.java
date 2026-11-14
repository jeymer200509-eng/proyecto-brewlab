package com.brewlab.data;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.data.repositories.UserSettingsRepository;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.UserSettings;
import com.brewlab.security.CsrfTokens;
import com.brewlab.security.JwtService;
import com.brewlab.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAndRepositoryTest {

    @Test
    void password_hasher_and_jwt_and_csrf_and_user_repositories_work_end_to_end() {
        try (DataSourceFactory.ManagedDataSource mds = DataSourceFactory.createForTests(null)) {
            SchemaMigrator migrator = new SchemaMigrator(mds.getDataSource());
            int applied = migrator.migrate();
            assertTrue(applied >= 1);
            assertEquals(0, migrator.migrate(), "La segunda ejecución debe ser idempotente");

            PasswordHasher hasher = new PasswordHasher();
            String hash = hasher.hash("CafeEspecial2026!");
            assertTrue(hasher.verify("CafeEspecial2026!", hash));
            assertFalse(hasher.verify("OtraClave", hash));

            UserRepository userRepo = new UserRepository(mds.getDataSource());
            Instant now = Instant.parse("2026-11-13T20:00:00Z");
            String userId = Ids.newId();
            User created = userRepo.create(userId, "  Catador@BrewLab.co ", "Valentina Ríos", hash, now);
            assertEquals("catador@brewlab.co", created.getEmail());

            String secret = Base64.getEncoder().encodeToString(
                    "0123456789abcdef0123456789abcdef0123456789abcdef".getBytes());
            JwtService jwtService = new JwtService(secret, 60);
            String token = jwtService.issueToken(created, now);
            JwtService.VerifiedToken verified = jwtService.verify(token, now.plusSeconds(30));
            assertNotNull(verified);
            assertEquals(userId, verified.getUserId());
            assertEquals(1, verified.getTokenVersion());
            assertNull(jwtService.verify(token, now.plusSeconds(3601)), "El token expirado debe rechazarse");
            assertNull(jwtService.verify(token + "corrupt", now.plusSeconds(30)));

            userRepo.incrementTokenVersion(userId, now.plusSeconds(10));
            User afterLogout = userRepo.findById(userId);
            assertEquals(2, afterLogout.getTokenVersion());

            UserSettingsRepository settingsRepo = new UserSettingsRepository(mds.getDataSource());
            UserSettings defaults = settingsRepo.findOrDefault(userId);
            assertFalse(defaults.isSoundEnabled());
            assertEquals(UserSettings.TimerTheme.AUTO, defaults.getTimerTheme());

            UserSettings updated = new UserSettings(userId, true, true, UserSettings.TimerTheme.DARK, now);
            settingsRepo.save(updated, now);
            UserSettings reloaded = settingsRepo.findOrDefault(userId);
            assertTrue(reloaded.isSoundEnabled());
            assertTrue(reloaded.isKeepScreenAwake());
            assertEquals(UserSettings.TimerTheme.DARK, reloaded.getTimerTheme());

            String csrf = CsrfTokens.generate();
            assertTrue(CsrfTokens.matches(csrf, csrf));
            assertFalse(CsrfTokens.matches(csrf, "token-distinto-1234567890"));
        }
    }
}
