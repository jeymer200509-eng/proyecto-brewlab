package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.Experiment;
import com.brewlab.domain.model.ExperimentTrial;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio JDBC para {@link Experiment} y sus ensayos vinculados a sesiones
 * ({@link ExperimentTrial}).
 */
public final class ExperimentRepository {
    private final DataSource dataSource;

    public ExperimentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void create(Experiment exp) {
        String sql = "INSERT INTO experiments (id, user_id, coffee_lot_id, roast_batch_id, title, question, "
                + "objective, hypothesis, modified_variable, controlled_variables, status, user_conclusion, "
                + "is_demo, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, exp.getId());
                    ps.setString(2, exp.getUserId());
                    ps.setString(3, exp.getCoffeeLotId());
                    ps.setString(4, exp.getRoastBatchId());
                    ps.setString(5, exp.getTitle());
                    ps.setString(6, exp.getQuestion());
                    ps.setString(7, exp.getObjective());
                    ps.setString(8, exp.getHypothesis());
                    ps.setString(9, exp.getModifiedVariable());
                    ps.setString(10, exp.getControlledVariables());
                    ps.setString(11, exp.getStatus());
                    ps.setString(12, exp.getUserConclusion());
                    ps.setBoolean(13, exp.isDemo());
                    JdbcUtils.setNullableTimestamp(ps, 14, exp.getCreatedAt());
                    JdbcUtils.setNullableTimestamp(ps, 15, exp.getUpdatedAt());
                    ps.executeUpdate();
                }
                for (ExperimentTrial t : exp.getTrials()) {
                    insertTrial(c, exp.getId(), t);
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el experimento", e);
        }
    }

    public Experiment addTrial(String userId, String experimentId, ExperimentTrial trial, Instant updatedAt) {
        Experiment existing = findById(userId, experimentId);
        if (existing == null) {
            return null;
        }
        try (Connection c = dataSource.getConnection()) {
            insertTrial(c, experimentId, trial);
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE experiments SET updated_at = ? WHERE id = ? AND user_id = ?")) {
                JdbcUtils.setNullableTimestamp(up, 1, updatedAt);
                up.setString(2, experimentId);
                up.setString(3, userId);
                up.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo agregar el ensayo al experimento", e);
        }
        return findById(userId, experimentId);
    }

    public Experiment updateConclusionAndStatus(String userId, String experimentId,
                                                String userConclusion, String status, Instant updatedAt) {
        String sql = "UPDATE experiments SET user_conclusion = ?, status = ?, updated_at = ? "
                + "WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userConclusion);
            ps.setString(2, status != null ? status : Experiment.STATUS_CONCLUDED);
            JdbcUtils.setNullableTimestamp(ps, 3, updatedAt);
            ps.setString(4, experimentId);
            ps.setString(5, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la conclusión del experimento", e);
        }
        return findById(userId, experimentId);
    }

    public void delete(String userId, String experimentId) {
        String sql = "DELETE FROM experiments WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, experimentId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar el experimento", e);
        }
    }

    public Experiment findById(String userId, String experimentId) {
        if (userId == null || experimentId == null) {
            return null;
        }
        String sql = "SELECT * FROM experiments WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, experimentId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapExperiment(c, rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando experimento", e);
        }
    }

    public List<Experiment> findAllByUser(String userId) {
        String sql = "SELECT * FROM experiments WHERE user_id = ? ORDER BY created_at DESC";
        List<Experiment> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapExperiment(c, rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando experimentos", e);
        }
    }

    private static void insertTrial(Connection c, String experimentId, ExperimentTrial t) throws SQLException {
        String sql = "INSERT INTO experiment_trials "
                + "(id, experiment_id, brew_session_id, trial_order, label, modified_value, notes, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getId() != null ? t.getId() : Ids.newId());
            ps.setString(2, experimentId);
            ps.setString(3, t.getBrewSessionId());
            ps.setInt(4, t.getTrialOrder());
            ps.setString(5, t.getLabel());
            ps.setString(6, t.getModifiedValue());
            ps.setString(7, t.getNotes());
            JdbcUtils.setNullableTimestamp(ps, 8, t.getCreatedAt());
            ps.executeUpdate();
        }
    }

    private static Experiment mapExperiment(Connection c, ResultSet rs) throws SQLException {
        String expId = rs.getString("id");
        List<ExperimentTrial> trials = loadTrials(c, expId);
        return new Experiment(
                expId,
                rs.getString("user_id"),
                rs.getString("coffee_lot_id"),
                rs.getString("roast_batch_id"),
                rs.getString("title"),
                rs.getString("question"),
                rs.getString("objective"),
                rs.getString("hypothesis"),
                rs.getString("modified_variable"),
                rs.getString("controlled_variables"),
                rs.getString("status"),
                rs.getString("user_conclusion"),
                rs.getBoolean("is_demo"),
                trials,
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }

    private static List<ExperimentTrial> loadTrials(Connection c, String experimentId) throws SQLException {
        String sql = "SELECT * FROM experiment_trials WHERE experiment_id = ? ORDER BY trial_order ASC, created_at ASC";
        List<ExperimentTrial> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, experimentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ExperimentTrial(
                            rs.getString("id"),
                            rs.getString("experiment_id"),
                            rs.getString("brew_session_id"),
                            rs.getInt("trial_order"),
                            rs.getString("label"),
                            rs.getString("modified_value"),
                            rs.getString("notes"),
                            JdbcUtils.getNullableInstant(rs, "created_at")
                    ));
                }
            }
        }
        return list;
    }
}
