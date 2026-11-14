package com.brewlab.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Properties;

/** Configuración de ejecución cargada desde WEB-INF/config/brewlab.properties o propiedades del sistema. */
public final class AppConfig {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final int dbPoolSize;
    private final String testDbUrl;
    private final String testDbUser;
    private final String testDbPassword;
    private final String jwtSecretBase64;
    private final int jwtTtlMinutes;
    private final ZoneId timezone;
    private final File dataDir;

    public AppConfig(String dbUrl, String dbUser, String dbPassword, int dbPoolSize,
                     String testDbUrl, String testDbUser, String testDbPassword,
                     String jwtSecretBase64, int jwtTtlMinutes, ZoneId timezone, File dataDir) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbPoolSize = dbPoolSize;
        this.testDbUrl = testDbUrl;
        this.testDbUser = testDbUser;
        this.testDbPassword = testDbPassword;
        this.jwtSecretBase64 = jwtSecretBase64;
        this.jwtTtlMinutes = jwtTtlMinutes;
        this.timezone = timezone;
        this.dataDir = dataDir;
    }

    public static AppConfig load(File webAppRoot) {
        Properties props = new Properties();
        String explicitPath = System.getProperty("brewlab.config");
        File configFile = explicitPath != null
                ? new File(explicitPath)
                : new File(webAppRoot, "WEB-INF/config/brewlab.properties");
        if (configFile.isFile()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + configFile.getAbsolutePath(), e);
            }
        }
        String dbUrl = prop(props, "db.url", "jdbc:mysql://localhost:3306/brewlab?useUnicode=true&characterEncoding=utf8");
        String dbUser = prop(props, "db.user", "brewlab");
        String dbPassword = prop(props, "db.password", "");
        int poolSize = Integer.parseInt(prop(props, "db.poolSize", "8"));

        String testDbUrl = prop(props, "test.db.url", "");
        String testDbUser = prop(props, "test.db.user", dbUser);
        String testDbPassword = prop(props, "test.db.password", dbPassword);

        String jwtSecret = prop(props, "jwt.secret", "");
        if (jwtSecret.isEmpty()) {
            byte[] randomBytes = new byte[48];
            new SecureRandom().nextBytes(randomBytes);
            jwtSecret = Base64.getEncoder().encodeToString(randomBytes);
        }
        int jwtTtl = Integer.parseInt(prop(props, "jwt.ttlMinutes", "720"));
        ZoneId zone = ZoneId.of(prop(props, "app.timezone", "America/Bogota"));
        File dataDir = new File(webAppRoot, "WEB-INF/data");
        return new AppConfig(dbUrl, dbUser, dbPassword, poolSize,
                testDbUrl, testDbUser, testDbPassword, jwtSecret, jwtTtl, zone, dataDir);
    }

    private static String prop(Properties props, String key, String defaultValue) {
        String sys = System.getProperty("brewlab." + key);
        if (sys != null && !sys.trim().isEmpty()) {
            return sys.trim();
        }
        String val = props.getProperty(key);
        return val != null ? val.trim() : defaultValue;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public int getDbPoolSize() {
        return dbPoolSize;
    }

    public String getTestDbUrl() {
        return testDbUrl;
    }

    public String getTestDbUser() {
        return testDbUser;
    }

    public String getTestDbPassword() {
        return testDbPassword;
    }

    public String getJwtSecretBase64() {
        return jwtSecretBase64;
    }

    public int getJwtTtlMinutes() {
        return jwtTtlMinutes;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public File getDataDir() {
        return dataDir;
    }
}
