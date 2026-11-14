package com.brewlab.data.repositories;

import com.brewlab.data.db.JdbcUtils;
import com.brewlab.domain.Ids;
import com.brewlab.domain.model.AssessmentDescriptor;
import com.brewlab.domain.model.PreferenceRecord;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.SensoryDescriptor;
import com.brewlab.domain.sensory.OriginalFlavorTaxonomy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio JDBC para la taxonomía sensorial jerárquica ({@link SensoryDescriptor}),
 * evaluaciones descriptivas ({@link SensoryAssessment}) y registros afectivos separados
 * ({@link PreferenceRecord}).
 */
public final class SensoryRepository {
    private final DataSource dataSource;

    public SensoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Siembra de forma idempotente la taxonomía sensorial original {@code brewlab-v1.0}. */
    public void seedDefaultTaxonomyIfNeeded() {
        List<SensoryDescriptor> defaults = OriginalFlavorTaxonomy.defaultDescriptors();
        String checkSql = "SELECT id FROM sensory_descriptors WHERE id = ?";
        String insertSql = "INSERT INTO sensory_descriptors "
                + "(id, user_id, family, subfamily, term, color_hex, taxonomy_version, is_custom, created_at) "
                + "VALUES (?, NULL, ?, ?, ?, ?, ?, FALSE, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement checkPs = c.prepareStatement(checkSql);
             PreparedStatement insPs = c.prepareStatement(insertSql)) {
            for (SensoryDescriptor d : defaults) {
                checkPs.setString(1, d.getId());
                boolean exists;
                try (ResultSet rs = checkPs.executeQuery()) {
                    exists = rs.next();
                }
                if (!exists) {
                    insPs.setString(1, d.getId());
                    insPs.setString(2, d.getFamily());
                    insPs.setString(3, d.getSubfamily());
                    insPs.setString(4, d.getTerm());
                    insPs.setString(5, d.getColorHex());
                    insPs.setString(6, d.getTaxonomyVersion());
                    JdbcUtils.setNullableTimestamp(insPs, 7, d.getCreatedAt());
                    insPs.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error sembrando taxonomía sensorial BrewLab", e);
        }
    }

    public List<SensoryDescriptor> findAllDescriptors(String userId) {
        String sql = "SELECT * FROM sensory_descriptors WHERE user_id IS NULL OR user_id = ? "
                + "ORDER BY is_custom ASC, family ASC, subfamily ASC, term ASC";
        List<SensoryDescriptor> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDescriptor(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando descriptores sensoriales", e);
        }
    }

    public SensoryDescriptor findDescriptorById(String descriptorId) {
        String sql = "SELECT * FROM sensory_descriptors WHERE id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, descriptorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapDescriptor(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando descriptor sensorial", e);
        }
    }

    public void createCustomDescriptor(SensoryDescriptor custom) {
        String sql = "INSERT INTO sensory_descriptors "
                + "(id, user_id, family, subfamily, term, color_hex, taxonomy_version, is_custom, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, custom.getId());
            ps.setString(2, custom.getUserId());
            ps.setString(3, custom.getFamily());
            ps.setString(4, custom.getSubfamily());
            ps.setString(5, custom.getTerm());
            ps.setString(6, custom.getColorHex());
            ps.setString(7, custom.getTaxonomyVersion());
            JdbcUtils.setNullableTimestamp(ps, 8, custom.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear el descriptor personalizado", e);
        }
    }

    public void createAssessment(SensoryAssessment assessment) {
        String sql = "INSERT INTO sensory_assessments (id, user_id, coffee_lot_id, roast_batch_id, brew_session_id, "
                + "evaluator_name, assessed_on, attribute_scale, fragrance_aroma, flavor, acidity, sweetness, "
                + "bitterness, body_texture, aftertaste, balance, descriptive_notes, is_demo, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, assessment.getId());
                    ps.setString(2, assessment.getUserId());
                    ps.setString(3, assessment.getCoffeeLotId());
                    ps.setString(4, assessment.getRoastBatchId());
                    ps.setString(5, assessment.getBrewSessionId());
                    ps.setString(6, assessment.getEvaluatorName());
                    JdbcUtils.setNullableDate(ps, 7, assessment.getAssessedOn());
                    ps.setString(8, assessment.getAttributeScale());
                    JdbcUtils.setNullableDouble(ps, 9, assessment.getFragranceAroma());
                    JdbcUtils.setNullableDouble(ps, 10, assessment.getFlavor());
                    JdbcUtils.setNullableDouble(ps, 11, assessment.getAcidity());
                    JdbcUtils.setNullableDouble(ps, 12, assessment.getSweetness());
                    JdbcUtils.setNullableDouble(ps, 13, assessment.getBitterness());
                    JdbcUtils.setNullableDouble(ps, 14, assessment.getBodyTexture());
                    JdbcUtils.setNullableDouble(ps, 15, assessment.getAftertaste());
                    JdbcUtils.setNullableDouble(ps, 16, assessment.getBalance());
                    ps.setString(17, assessment.getDescriptiveNotes());
                    ps.setBoolean(18, assessment.isDemo());
                    JdbcUtils.setNullableTimestamp(ps, 19, assessment.getCreatedAt());
                    JdbcUtils.setNullableTimestamp(ps, 20, assessment.getUpdatedAt());
                    ps.executeUpdate();
                }

                insertAssessmentDescriptors(c, assessment.getId(), assessment.getDescriptors());
                insertPreferenceRecord(c, assessment.getPreferenceRecord());

                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la evaluación sensorial", e);
        }
    }

    public SensoryAssessment findById(String userId, String assessmentId) {
        if (userId == null || assessmentId == null) {
            return null;
        }
        String sql = "SELECT * FROM sensory_assessments WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assessmentId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapAssessment(c, rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando evaluación sensorial", e);
        }
    }

    public List<SensoryAssessment> findByBrewSessionId(String userId, String brewSessionId) {
        if (userId == null || brewSessionId == null) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM sensory_assessments WHERE user_id = ? AND brew_session_id = ? "
                + "ORDER BY assessed_on DESC, created_at DESC";
        List<SensoryAssessment> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, brewSessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAssessment(c, rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando evaluaciones por sesión", e);
        }
    }

    public List<SensoryAssessment> findAllByUser(String userId) {
        String sql = "SELECT * FROM sensory_assessments WHERE user_id = ? "
                + "ORDER BY assessed_on DESC, created_at DESC";
        List<SensoryAssessment> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAssessment(c, rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Error listando evaluaciones sensoriales", e);
        }
    }

    public void deleteAssessment(String userId, String assessmentId) {
        String sql = "DELETE FROM sensory_assessments WHERE id = ? AND user_id = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assessmentId);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar la evaluación sensorial", e);
        }
    }

    private static void insertAssessmentDescriptors(Connection c, String assessmentId,
                                                    List<AssessmentDescriptor> descriptors) throws SQLException {
        if (descriptors == null || descriptors.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO assessment_descriptors "
                + "(id, sensory_assessment_id, descriptor_id, intensity, scale, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (AssessmentDescriptor ad : descriptors) {
                ps.setString(1, ad.getId() != null ? ad.getId() : Ids.newId());
                ps.setString(2, assessmentId);
                ps.setString(3, ad.getDescriptorId());
                JdbcUtils.setNullableDouble(ps, 4, ad.getIntensity());
                ps.setString(5, ad.getScale() != null ? ad.getScale() : "1-5");
                ps.setString(6, ad.getNotes());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertPreferenceRecord(Connection c, PreferenceRecord pref) throws SQLException {
        if (pref == null) {
            return;
        }
        String sql = "INSERT INTO preference_records "
                + "(id, sensory_assessment_id, user_id, overall_liking, liking_scale, would_repeat, "
                + "affective_context, affective_notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pref.getId() != null ? pref.getId() : Ids.newId());
            ps.setString(2, pref.getSensoryAssessmentId());
            ps.setString(3, pref.getUserId());
            JdbcUtils.setNullableDouble(ps, 4, pref.getOverallLiking());
            ps.setString(5, pref.getLikingScale() != null ? pref.getLikingScale() : "1-9");
            ps.setString(6, pref.getWouldRepeat());
            ps.setString(7, pref.getAffectiveContext());
            ps.setString(8, pref.getAffectiveNotes());
            JdbcUtils.setNullableTimestamp(ps, 9, pref.getCreatedAt());
            ps.executeUpdate();
        }
    }

    private static SensoryAssessment mapAssessment(Connection c, ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        List<AssessmentDescriptor> descriptors = loadAssessmentDescriptors(c, id);
        PreferenceRecord pref = loadPreferenceRecord(c, id);
        return new SensoryAssessment(
                id,
                rs.getString("user_id"),
                rs.getString("coffee_lot_id"),
                rs.getString("roast_batch_id"),
                rs.getString("brew_session_id"),
                rs.getString("evaluator_name"),
                JdbcUtils.getNullableDate(rs, "assessed_on"),
                rs.getString("attribute_scale"),
                JdbcUtils.getNullableDouble(rs, "fragrance_aroma"),
                JdbcUtils.getNullableDouble(rs, "flavor"),
                JdbcUtils.getNullableDouble(rs, "acidity"),
                JdbcUtils.getNullableDouble(rs, "sweetness"),
                JdbcUtils.getNullableDouble(rs, "bitterness"),
                JdbcUtils.getNullableDouble(rs, "body_texture"),
                JdbcUtils.getNullableDouble(rs, "aftertaste"),
                JdbcUtils.getNullableDouble(rs, "balance"),
                rs.getString("descriptive_notes"),
                rs.getBoolean("is_demo"),
                descriptors,
                pref,
                JdbcUtils.getNullableInstant(rs, "created_at"),
                JdbcUtils.getNullableInstant(rs, "updated_at")
        );
    }

    private static List<AssessmentDescriptor> loadAssessmentDescriptors(Connection c, String assessmentId)
            throws SQLException {
        String sql = "SELECT ad.id AS ad_id, ad.sensory_assessment_id, ad.descriptor_id, ad.intensity, "
                + "ad.scale, ad.notes, sd.user_id AS sd_user_id, sd.family, sd.subfamily, sd.term, "
                + "sd.color_hex, sd.taxonomy_version, sd.is_custom, sd.created_at AS sd_created_at "
                + "FROM assessment_descriptors ad "
                + "JOIN sensory_descriptors sd ON sd.id = ad.descriptor_id "
                + "WHERE ad.sensory_assessment_id = ? "
                + "ORDER BY sd.family ASC, sd.subfamily ASC, sd.term ASC";
        List<AssessmentDescriptor> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SensoryDescriptor sd = new SensoryDescriptor(
                            rs.getString("descriptor_id"),
                            rs.getString("sd_user_id"),
                            rs.getString("family"),
                            rs.getString("subfamily"),
                            rs.getString("term"),
                            rs.getString("color_hex"),
                            rs.getString("taxonomy_version"),
                            rs.getBoolean("is_custom"),
                            JdbcUtils.getNullableInstant(rs, "sd_created_at")
                    );
                    list.add(new AssessmentDescriptor(
                            rs.getString("ad_id"),
                            rs.getString("sensory_assessment_id"),
                            rs.getString("descriptor_id"),
                            JdbcUtils.getNullableDouble(rs, "intensity"),
                            rs.getString("scale"),
                            rs.getString("notes"),
                            sd
                    ));
                }
            }
        }
        return list;
    }

    private static PreferenceRecord loadPreferenceRecord(Connection c, String assessmentId) throws SQLException {
        String sql = "SELECT * FROM preference_records WHERE sensory_assessment_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, assessmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PreferenceRecord(
                        rs.getString("id"),
                        rs.getString("sensory_assessment_id"),
                        rs.getString("user_id"),
                        JdbcUtils.getNullableDouble(rs, "overall_liking"),
                        rs.getString("liking_scale"),
                        rs.getString("would_repeat"),
                        rs.getString("affective_context"),
                        rs.getString("affective_notes"),
                        JdbcUtils.getNullableInstant(rs, "created_at")
                );
            }
        }
    }

    private static SensoryDescriptor mapDescriptor(ResultSet rs) throws SQLException {
        return new SensoryDescriptor(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("family"),
                rs.getString("subfamily"),
                rs.getString("term"),
                rs.getString("color_hex"),
                rs.getString("taxonomy_version"),
                rs.getBoolean("is_custom"),
                JdbcUtils.getNullableInstant(rs, "created_at")
        );
    }
}
