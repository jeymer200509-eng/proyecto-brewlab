package com.brewlab.data.db;

import com.brewlab.app.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Crea el pool JDBC (HikariCP). Intenta usar MySQL según {@link AppConfig}; si el esquema MySQL
 * externo aún no ha sido aprovisionado por el administrador local, activa automáticamente el motor
 * relacional SQL local en modo compatible MySQL sobre {@code WEB-INF/data/brewlab} para garantizar
 * persistencia real en disco sin configuración manual previa.
 */
public final class DataSourceFactory {
    private static final Logger LOG = Logger.getLogger(DataSourceFactory.class.getName());

    public static final class ManagedDataSource implements AutoCloseable {
        private final HikariDataSource dataSource;
        private final String engineLabel;
        private final String jdbcUrlSummary;
        private final boolean usingFallback;

        public ManagedDataSource(HikariDataSource dataSource, String engineLabel,
                                 String jdbcUrlSummary, boolean usingFallback) {
            this.dataSource = dataSource;
            this.engineLabel = engineLabel;
            this.jdbcUrlSummary = jdbcUrlSummary;
            this.usingFallback = usingFallback;
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public String getEngineLabel() {
            return engineLabel;
        }

        public String getJdbcUrlSummary() {
            return jdbcUrlSummary;
        }

        public boolean isUsingFallback() {
            return usingFallback;
        }

        @Override
        public void close() {
            dataSource.close();
        }
    }

    private DataSourceFactory() {
    }

    public static ManagedDataSource create(AppConfig config) {
        if (canConnectMySql(config.getDbUrl(), config.getDbUser(), config.getDbPassword())) {
            HikariConfig hc = new HikariConfig();
            hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hc.setJdbcUrl(config.getDbUrl());
            hc.setUsername(config.getDbUser());
            hc.setPassword(config.getDbPassword());
            hc.setMaximumPoolSize(config.getDbPoolSize());
            hc.setPoolName("BrewLab-MySQL");
            return new ManagedDataSource(new HikariDataSource(hc),
                    "MySQL Relacional (HikariCP)", sanitizeUrl(config.getDbUrl()), false);
        }

        File dataDir = config.getDataDir();
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            LOG.warning("No se pudo crear directorio de datos local: " + dataDir.getAbsolutePath());
        }
        File dbFile = new File(dataDir, "brewlab_db");
        String h2Url = "jdbc:h2:file:" + dbFile.getAbsolutePath().replace('\\', '/')
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
                + ";NON_KEYWORDS=VALUE,ORDER,USER,KEY;AUTO_SERVER=TRUE";
        LOG.info("Usando motor SQL relacional local en disco (compatible MySQL): " + dbFile.getAbsolutePath());
        HikariConfig hc = new HikariConfig();
        hc.setDriverClassName("org.h2.Driver");
        hc.setJdbcUrl(h2Url);
        hc.setUsername("sa");
        hc.setPassword("");
        hc.setMaximumPoolSize(config.getDbPoolSize());
        hc.setPoolName("BrewLab-LocalSQL");
        return new ManagedDataSource(new HikariDataSource(hc),
                "SQL Relacional Local (modo MySQL en disco)", "WEB-INF/data/brewlab_db", true);
    }

    public static ManagedDataSource createForTests(AppConfig config) {
        if (config != null && !config.getTestDbUrl().isEmpty()
                && canConnectMySql(config.getTestDbUrl(), config.getTestDbUser(), config.getTestDbPassword())) {
            HikariConfig hc = new HikariConfig();
            hc.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hc.setJdbcUrl(config.getTestDbUrl());
            hc.setUsername(config.getTestDbUser());
            hc.setPassword(config.getTestDbPassword());
            hc.setMaximumPoolSize(4);
            hc.setPoolName("BrewLab-TestMySQL");
            return new ManagedDataSource(new HikariDataSource(hc),
                    "MySQL Test", sanitizeUrl(config.getTestDbUrl()), false);
        }
        String memName = "brewlab_test_" + UUID.randomUUID().toString().replace("-", "");
        String h2Url = "jdbc:h2:mem:" + memName
                + ";DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
                + ";NON_KEYWORDS=VALUE,ORDER,USER,KEY";
        HikariConfig hc = new HikariConfig();
        hc.setDriverClassName("org.h2.Driver");
        hc.setJdbcUrl(h2Url);
        hc.setUsername("sa");
        hc.setPassword("");
        hc.setMaximumPoolSize(4);
        hc.setPoolName("BrewLab-TestSQL");
        return new ManagedDataSource(new HikariDataSource(hc),
                "SQL Test (modo MySQL)", memName, true);
    }

    private static boolean canConnectMySql(String url, String user, String password) {
        if (url == null || !url.startsWith("jdbc:mysql:")) {
            return false;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(2);
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                return conn.isValid(2);
            }
        } catch (ClassNotFoundException | SQLException e) {
            LOG.log(Level.FINE, "MySQL externo no disponible aún (" + e.getMessage() + "); se usará SQL local.", e);
            return false;
        }
    }

    private static String sanitizeUrl(String url) {
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }
}
