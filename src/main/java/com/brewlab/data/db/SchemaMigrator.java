package com.brewlab.data.db;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Ejecutor de migraciones SQL versionadas. Registra cada versión aplicada en
 * {@code schema_migrations} con suma SHA-256 para garantizar integridad del esquema.
 */
public final class SchemaMigrator {

    /** Lista ordenada de recursos de migración en {@code /db/migration/}. */
    private static final List<String> MIGRATIONS = Collections.unmodifiableList(Arrays.asList(
            "V001__base_auth_and_settings.sql"
    ));

    private final DataSource dataSource;
    private final List<String> migrationFiles;

    public SchemaMigrator(DataSource dataSource) {
        this(dataSource, MIGRATIONS);
    }

    public SchemaMigrator(DataSource dataSource, List<String> migrationFiles) {
        this.dataSource = dataSource;
        this.migrationFiles = new ArrayList<>(migrationFiles);
    }

    public int migrate() {
        ensureTrackingTable();
        int appliedCount = 0;
        for (String resourceName : migrationFiles) {
            int sep = resourceName.indexOf("__");
            if (sep <= 1 || !resourceName.startsWith("V") || !resourceName.endsWith(".sql")) {
                throw new IllegalStateException("Nombre de migración inválido: " + resourceName);
            }
            int version = Integer.parseInt(resourceName.substring(1, sep));
            String name = resourceName.substring(sep + 2, resourceName.length() - 4);
            String sql = readResource("/db/migration/" + resourceName);
            String checksum = sha256Hex(sql);

            String existingChecksum = findChecksum(version);
            if (existingChecksum != null) {
                if (!existingChecksum.equals(checksum)) {
                    throw new IllegalStateException("Checksum de migración V" + version
                            + " cambió respecto al aplicado previamente.");
                }
                continue;
            }
            applyMigration(version, name, checksum, sql);
            appliedCount++;
        }
        return appliedCount;
    }

    public int currentVersion() {
        ensureTrackingTable();
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_migrations")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando versión de esquema", e);
        }
    }

    private void ensureTrackingTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS schema_migrations ("
                + "version INT NOT NULL,"
                + "name VARCHAR(150) NOT NULL,"
                + "checksum VARCHAR(64) NOT NULL,"
                + "applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (version)"
                + ")";
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear tabla schema_migrations", e);
        }
    }

    private String findChecksum(int version) {
        String q = "SELECT checksum FROM schema_migrations WHERE version = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar migración V" + version, e);
        }
    }

    private void applyMigration(int version, String name, String checksum, String sqlScript) {
        List<String> statements = splitSqlStatements(sqlScript);
        try (Connection c = dataSource.getConnection()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) {
                    for (String stmt : statements) {
                        st.execute(stmt);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO schema_migrations (version, name, checksum) VALUES (?, ?, ?)")) {
                    ps.setInt(1, version);
                    ps.setString(2, name);
                    ps.setString(3, checksum);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error aplicando migración V" + version + " (" + name + ")", e);
        }
    }

    static List<String> splitSqlStatements(String script) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : script.split("\r?\n")) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(rawLine).append('\n');
            if (trimmed.endsWith(";")) {
                String stmt = current.toString().trim();
                stmt = stmt.substring(0, stmt.length() - 1).trim();
                if (!stmt.isEmpty()) {
                    result.add(stmt);
                }
                current.setLength(0);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        return result;
    }

    private static String readResource(String path) {
        InputStream in = SchemaMigrator.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Recurso SQL no encontrado: " + path);
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo recurso SQL: " + path, e);
        }
        return sb.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
