package com.brewlab.data.backup;

import com.brewlab.data.repositories.BrewSessionRepository;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.EquipmentRepository;
import com.brewlab.data.repositories.ExperimentRepository;
import com.brewlab.data.repositories.RecipeRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.SensoryRepository;
import com.brewlab.data.repositories.WaterProfileRepository;
import com.brewlab.domain.model.AssessmentDescriptor;
import com.brewlab.domain.model.BrewEvent;
import com.brewlab.domain.model.BrewSession;
import com.brewlab.domain.model.CoffeeLot;
import com.brewlab.domain.model.Equipment;
import com.brewlab.domain.model.Experiment;
import com.brewlab.domain.model.ExperimentTrial;
import com.brewlab.domain.model.PreferenceRecord;
import com.brewlab.domain.model.Recipe;
import com.brewlab.domain.model.RoastBatch;
import com.brewlab.domain.model.SensoryAssessment;
import com.brewlab.domain.model.SensoryDescriptor;
import com.brewlab.domain.model.WaterProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servicio de respaldo manual JSON (con versión de esquema, prevalidación y vista previa antes de
 * restaurar) y exportación CSV auditable (con {@code null} como celda vacía, nunca {@code 0}).
 */
public final class BackupService {
    public static final String FORMAT_ID = "brewlab-backup";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                    (JsonSerializer<Instant>) (src, typeOfSrc, ctx) -> src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class,
                    (JsonDeserializer<Instant>) (json, typeOfT, ctx) ->
                            json == null || json.isJsonNull() || json.getAsString().trim().isEmpty()
                                    ? null : Instant.parse(json.getAsString().trim()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (src, typeOfSrc, ctx) -> src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, typeOfT, ctx) ->
                            json == null || json.isJsonNull() || json.getAsString().trim().isEmpty()
                                    ? null : LocalDate.parse(json.getAsString().trim()))
            .setPrettyPrinting()
            .create();

    public static final class BackupPayload {
        public String format;
        public int schemaVersion;
        public String exportedAt;
        public List<CoffeeLot> coffeeLots = new ArrayList<>();
        public List<RoastBatch> roastBatches = new ArrayList<>();
        public List<Equipment> equipment = new ArrayList<>();
        public List<WaterProfile> waterProfiles = new ArrayList<>();
        public List<Recipe> recipes = new ArrayList<>();
        public List<BrewSession> brewSessions = new ArrayList<>();
        public List<SensoryDescriptor> customDescriptors = new ArrayList<>();
        public List<SensoryAssessment> sensoryAssessments = new ArrayList<>();
        public List<Experiment> experiments = new ArrayList<>();
    }

    public static final class ImportPreview {
        private final boolean valid;
        private final int schemaVersion;
        private final int coffeeLotsCount;
        private final int roastBatchesCount;
        private final int equipmentCount;
        private final int waterProfilesCount;
        private final int recipesCount;
        private final int sessionsCount;
        private final int assessmentsCount;
        private final int experimentsCount;
        private final String errorMessage;

        public ImportPreview(boolean valid, int schemaVersion, int coffeeLotsCount, int roastBatchesCount,
                             int equipmentCount, int waterProfilesCount, int recipesCount, int sessionsCount,
                             int assessmentsCount, int experimentsCount, String errorMessage) {
            this.valid = valid;
            this.schemaVersion = schemaVersion;
            this.coffeeLotsCount = coffeeLotsCount;
            this.roastBatchesCount = roastBatchesCount;
            this.equipmentCount = equipmentCount;
            this.waterProfilesCount = waterProfilesCount;
            this.recipesCount = recipesCount;
            this.sessionsCount = sessionsCount;
            this.assessmentsCount = assessmentsCount;
            this.experimentsCount = experimentsCount;
            this.errorMessage = errorMessage;
        }

        public static ImportPreview error(String message) {
            return new ImportPreview(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, message);
        }

        public boolean isValid() { return valid; }
        public int getSchemaVersion() { return schemaVersion; }
        public int getCoffeeLotsCount() { return coffeeLotsCount; }
        public int getRoastBatchesCount() { return roastBatchesCount; }
        public int getEquipmentCount() { return equipmentCount; }
        public int getWaterProfilesCount() { return waterProfilesCount; }
        public int getRecipesCount() { return recipesCount; }
        public int getSessionsCount() { return sessionsCount; }
        public int getAssessmentsCount() { return assessmentsCount; }
        public int getExperimentsCount() { return experimentsCount; }
        public String getErrorMessage() { return errorMessage; }
    }

    private final DataSource dataSource;
    private final CoffeeLotRepository coffeeLotRepository;
    private final RoastBatchRepository roastBatchRepository;
    private final EquipmentRepository equipmentRepository;
    private final WaterProfileRepository waterProfileRepository;
    private final RecipeRepository recipeRepository;
    private final BrewSessionRepository brewSessionRepository;
    private final SensoryRepository sensoryRepository;
    private final ExperimentRepository experimentRepository;

    public BackupService(DataSource dataSource,
                         CoffeeLotRepository coffeeLotRepository,
                         RoastBatchRepository roastBatchRepository,
                         EquipmentRepository equipmentRepository,
                         WaterProfileRepository waterProfileRepository,
                         RecipeRepository recipeRepository,
                         BrewSessionRepository brewSessionRepository,
                         SensoryRepository sensoryRepository,
                         ExperimentRepository experimentRepository) {
        this.dataSource = dataSource;
        this.coffeeLotRepository = coffeeLotRepository;
        this.roastBatchRepository = roastBatchRepository;
        this.equipmentRepository = equipmentRepository;
        this.waterProfileRepository = waterProfileRepository;
        this.recipeRepository = recipeRepository;
        this.brewSessionRepository = brewSessionRepository;
        this.sensoryRepository = sensoryRepository;
        this.experimentRepository = experimentRepository;
    }

    public String exportJson(String userId, int schemaVersion, Instant exportedAt) {
        BackupPayload payload = new BackupPayload();
        payload.format = FORMAT_ID;
        payload.schemaVersion = schemaVersion;
        payload.exportedAt = exportedAt != null ? exportedAt.toString() : Instant.now().toString();
        payload.coffeeLots = coffeeLotRepository.findAll(userId, null, true);
        payload.roastBatches = roastBatchRepository.findAllByUser(userId, true);
        payload.equipment = equipmentRepository.findAllByUser(userId);
        payload.waterProfiles = waterProfileRepository.findAllByUser(userId);
        payload.recipes = recipeRepository.findAllByUser(userId);
        payload.brewSessions = brewSessionRepository.findAllByUser(userId);

        List<SensoryDescriptor> allDescs = sensoryRepository.findAllDescriptors(userId);
        List<SensoryDescriptor> customOnly = new ArrayList<>();
        for (SensoryDescriptor d : allDescs) {
            if (d.isCustom() && userId.equals(d.getUserId())) {
                customOnly.add(d);
            }
        }
        payload.customDescriptors = customOnly;
        payload.sensoryAssessments = sensoryRepository.findAllByUser(userId);
        payload.experiments = experimentRepository.findAllByUser(userId);
        return GSON.toJson(payload);
    }

    /**
     * Prevalida un archivo JSON de respaldo sin modificar los datos existentes.
     */
    public ImportPreview prevalidate(String rawJson, int maxSupportedSchemaVersion) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return ImportPreview.error("El contenido JSON está vacío.");
        }
        try {
            JsonElement rootEl = JsonParser.parseString(rawJson);
            if (!rootEl.isJsonObject()) {
                return ImportPreview.error("El documento JSON debe ser un objeto raíz válido.");
            }
            JsonObject root = rootEl.getAsJsonObject();
            if (!root.has("format") || !FORMAT_ID.equals(root.get("format").getAsString())) {
                return ImportPreview.error("Formato de respaldo no reconocido (se esperaba '" + FORMAT_ID + "').");
            }
            if (!root.has("schemaVersion") || !root.get("schemaVersion").isJsonPrimitive()) {
                return ImportPreview.error("El respaldo no declara 'schemaVersion'.");
            }
            int version = root.get("schemaVersion").getAsInt();
            if (version < 1 || version > maxSupportedSchemaVersion) {
                return ImportPreview.error("Versión de esquema incompatible (schemaVersion=" + version
                        + "; versiones soportadas: 1 a " + maxSupportedSchemaVersion + ").");
            }

            BackupPayload parsed = GSON.fromJson(root, BackupPayload.class);
            if (parsed == null) {
                return ImportPreview.error("No se pudo deserializar la estructura del respaldo.");
            }

            return new ImportPreview(
                    true,
                    version,
                    sizeOf(parsed.coffeeLots),
                    sizeOf(parsed.roastBatches),
                    sizeOf(parsed.equipment),
                    sizeOf(parsed.waterProfiles),
                    sizeOf(parsed.recipes),
                    sizeOf(parsed.brewSessions),
                    sizeOf(parsed.sensoryAssessments),
                    sizeOf(parsed.experiments),
                    null
            );
        } catch (RuntimeException e) {
            return ImportPreview.error("JSON inválido o corrupto: " + e.getMessage());
        }
    }

    /**
     * Restaura un respaldo JSON previamente validado para el usuario indicado.
     * Si la prevalidación falla, lanza {@link IllegalArgumentException} sin alterar los datos actuales.
     */
    public ImportPreview restoreJson(String userId, String rawJson, int maxSupportedSchemaVersion) {
        ImportPreview preview = prevalidate(rawJson, maxSupportedSchemaVersion);
        if (!preview.isValid()) {
            throw new IllegalArgumentException(preview.getErrorMessage());
        }
        BackupPayload payload = GSON.fromJson(rawJson, BackupPayload.class);
        clearUserData(userId);
        sensoryRepository.seedDefaultTaxonomyIfNeeded();

        if (payload.coffeeLots != null) {
            for (CoffeeLot l : payload.coffeeLots) {
                coffeeLotRepository.create(new CoffeeLot(
                        l.getId(), userId, l.getCode(), l.getName(), l.getCountry(), l.getRegion(),
                        l.getMunicipality(), l.getFarm(), l.getProducer(), l.getSpecies(), l.getVariety(),
                        l.getProcess(), l.getAltitudeMin(), l.getAltitudeMax(), l.getHarvest(),
                        l.getReceivedOn(), l.getMoisturePct(), l.getDensity(), l.getScreenSize(),
                        l.getNotes(), l.isDemo(), l.getArchivedAt(), l.getCreatedAt(), l.getUpdatedAt()
                ));
            }
        }
        if (payload.roastBatches != null) {
            for (RoastBatch b : payload.roastBatches) {
                roastBatchRepository.create(new RoastBatch(
                        b.getId(), userId, b.getCoffeeLotId(), b.getBatchCode(), b.getRoastDate(),
                        b.getRoastLevel(), b.getRoaster(), b.getMachine(), b.getRoastNotes(),
                        b.getWeightLossPct(), b.getAgtron(), b.getOpenedOn(),
                        b.getPreferredByDate(), b.getAvailableWeightG(), b.isDemo(),
                        b.getArchivedAt(), b.getCreatedAt(), b.getUpdatedAt()
                ));
            }
        }
        if (payload.equipment != null) {
            for (Equipment eq : payload.equipment) {
                equipmentRepository.create(new Equipment(
                        eq.getId(), userId, eq.getType(), eq.getBrand(), eq.getModel(),
                        eq.getName(), eq.getSettingsScale(), eq.getNotes(),
                        eq.isDemo(), eq.getCreatedAt(), eq.getUpdatedAt()
                ));
            }
        }
        if (payload.waterProfiles != null) {
            for (WaterProfile wp : payload.waterProfiles) {
                waterProfileRepository.create(new WaterProfile(
                        wp.getId(), userId, wp.getName(), wp.getSource(), wp.getTdsPpm(),
                        wp.getHardnessGh(), wp.getAlkalinityKh(), wp.getMinerals(),
                        wp.getMeasuredAt(), wp.getNotes(), wp.isDemo(),
                        wp.getCreatedAt(), wp.getUpdatedAt()
                ));
            }
        }
        if (payload.recipes != null) {
            for (Recipe r : payload.recipes) {
                recipeRepository.create(new Recipe(
                        r.getId(), userId, r.getName(), r.getMethod(), r.getCoffeeLotId(),
                        r.getRoastBatchId(), r.getDoseG(), r.getWaterG(), r.getTemperatureC(),
                        r.getGrindSetting(), r.getTargetTimeMinS(), r.getTargetTimeMaxS(),
                        r.getEquipmentIds(), r.getWaterProfileId(), r.getNotes(),
                        r.getVersion(), r.isDemo(), r.getSteps(), r.getCreatedAt(), r.getUpdatedAt()
                ));
            }
        }
        if (payload.brewSessions != null) {
            for (BrewSession s : payload.brewSessions) {
                brewSessionRepository.create(new BrewSession(
                        s.getId(), userId, s.getRecipeId(), s.getCoffeeLotId(), s.getRoastBatchId(),
                        s.getWaterProfileId(), s.getEquipmentIds(), s.getStatus(), s.getStartedAt(),
                        s.getPausedAt(), s.getAccumulatedPauseMs(), s.getCompletedAt(),
                        s.getPlannedRecipeName(), s.getPlannedMethod(), s.getPlannedDoseG(),
                        s.getPlannedWaterG(), s.getPlannedTemperatureC(), s.getPlannedGrindSetting(),
                        s.getPlannedTargetTimeMinS(), s.getPlannedTargetTimeMaxS(),
                        s.getRecipeSnapshotJson(), s.getActualDoseG(), s.getActualWaterG(),
                        s.getActualTemperatureC(), s.getActualGrindSetting(), s.getTotalTimeSeconds(),
                        s.getFinalBeverageWeightG(), s.getNotes(), s.isDemo(),
                        s.getEvents() != null ? s.getEvents() : Collections.<BrewEvent>emptyList(),
                        s.getCreatedAt(), s.getUpdatedAt()
                ));
            }
        }
        if (payload.customDescriptors != null) {
            for (SensoryDescriptor cd : payload.customDescriptors) {
                sensoryRepository.createCustomDescriptor(new SensoryDescriptor(
                        cd.getId(), userId, cd.getFamily(), cd.getSubfamily(), cd.getTerm(),
                        cd.getColorHex(), cd.getTaxonomyVersion(), true, cd.getCreatedAt()
                ));
            }
        }
        if (payload.sensoryAssessments != null) {
            for (SensoryAssessment sa : payload.sensoryAssessments) {
                PreferenceRecord pr = sa.getPreferenceRecord();
                PreferenceRecord reboundPr = pr == null ? null : new PreferenceRecord(
                        pr.getId(), sa.getId(), userId, pr.getOverallLiking(), pr.getLikingScale(),
                        pr.getWouldRepeat(), pr.getAffectiveContext(), pr.getAffectiveNotes(), pr.getCreatedAt()
                );
                sensoryRepository.createAssessment(new SensoryAssessment(
                        sa.getId(), userId, sa.getCoffeeLotId(), sa.getRoastBatchId(), sa.getBrewSessionId(),
                        sa.getEvaluatorName(), sa.getAssessedOn(), sa.getAttributeScale(),
                        sa.getFragranceAroma(), sa.getFlavor(), sa.getAcidity(), sa.getSweetness(),
                        sa.getBitterness(), sa.getBodyTexture(), sa.getAftertaste(), sa.getBalance(),
                        sa.getDescriptiveNotes(), sa.isDemo(),
                        sa.getDescriptors() != null ? sa.getDescriptors() : Collections.<AssessmentDescriptor>emptyList(),
                        reboundPr, sa.getCreatedAt(), sa.getUpdatedAt()
                ));
            }
        }
        if (payload.experiments != null) {
            for (Experiment exp : payload.experiments) {
                experimentRepository.create(new Experiment(
                        exp.getId(), userId, exp.getCoffeeLotId(), exp.getRoastBatchId(),
                        exp.getTitle(), exp.getQuestion(), exp.getObjective(), exp.getHypothesis(),
                        exp.getModifiedVariable(), exp.getControlledVariables(), exp.getStatus(),
                        exp.getUserConclusion(), exp.isDemo(),
                        exp.getTrials() != null ? exp.getTrials() : Collections.<ExperimentTrial>emptyList(),
                        exp.getCreatedAt(), exp.getUpdatedAt()
                ));
            }
        }
        return preview;
    }

    public void clearUserData(String userId) {
        String[] statements = {
                "DELETE FROM experiments WHERE user_id = ?",
                "DELETE FROM sensory_assessments WHERE user_id = ?",
                "DELETE FROM sensory_descriptors WHERE user_id = ?",
                "DELETE FROM brew_sessions WHERE user_id = ?",
                "DELETE FROM recipes WHERE user_id = ?",
                "DELETE FROM water_profiles WHERE user_id = ?",
                "DELETE FROM equipment WHERE user_id = ?",
                "DELETE FROM roast_batches WHERE user_id = ?",
                "DELETE FROM coffee_lots WHERE user_id = ?"
        };
        try (Connection c = dataSource.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                for (String sql : statements) {
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setString(1, userId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron limpiar los datos del usuario", e);
        }
    }

    /**
     * Exporta una tabla del usuario en formato CSV RFC 4180, dejando los campos {@code null}
     * vacíos (nunca {@code 0}).
     */
    public String exportCsv(String userId, String tableName) {
        StringBuilder sb = new StringBuilder();
        if ("coffee_lots".equalsIgnoreCase(tableName)) {
            sb.append("id,code,name,country,region,municipality,farm,producer,species,variety,process,altitude_min_m,altitude_max_m,harvest,moisture_pct,density,screen_size,is_demo\r\n");
            for (CoffeeLot l : coffeeLotRepository.findAll(userId, null, true)) {
                row(sb, l.getId(), l.getCode(), l.getName(), l.getCountry(), l.getRegion(),
                        l.getMunicipality(), l.getFarm(), l.getProducer(), l.getSpecies(),
                        l.getVariety(), l.getProcess(), l.getAltitudeMin(), l.getAltitudeMax(),
                        l.getHarvest(), l.getMoisturePct(), l.getDensity(), l.getScreenSize(),
                        Boolean.valueOf(l.isDemo()));
            }
        } else if ("roast_batches".equalsIgnoreCase(tableName)) {
            sb.append("id,coffee_lot_id,code,roast_date,roast_level,roaster_name,machine_model,weight_loss_pct,agtron,initial_weight_g,is_demo\r\n");
            for (RoastBatch b : roastBatchRepository.findAllByUser(userId, true)) {
                row(sb, b.getId(), b.getCoffeeLotId(), b.getBatchCode(), b.getRoastDate(),
                        b.getRoastLevel(), b.getRoaster(), b.getMachine(),
                        b.getWeightLossPct(), b.getAgtron(), b.getAvailableWeightG(),
                        Boolean.valueOf(b.isDemo()));
            }
        } else if ("sensory_assessments".equalsIgnoreCase(tableName)) {
            sb.append("id,brew_session_id,coffee_lot_id,roast_batch_id,evaluator_name,assessed_on,attribute_scale,fragrance_aroma,flavor,acidity,sweetness,bitterness,body_texture,aftertaste,balance,overall_liking,liking_scale,would_repeat,is_demo\r\n");
            for (SensoryAssessment a : sensoryRepository.findAllByUser(userId)) {
                PreferenceRecord pr = a.getPreferenceRecord();
                row(sb, a.getId(), a.getBrewSessionId(), a.getCoffeeLotId(), a.getRoastBatchId(),
                        a.getEvaluatorName(), a.getAssessedOn(), a.getAttributeScale(),
                        a.getFragranceAroma(), a.getFlavor(), a.getAcidity(), a.getSweetness(),
                        a.getBitterness(), a.getBodyTexture(), a.getAftertaste(), a.getBalance(),
                        pr != null ? pr.getOverallLiking() : null,
                        pr != null ? pr.getLikingScale() : null,
                        pr != null ? pr.getWouldRepeat() : null,
                        Boolean.valueOf(a.isDemo()));
            }
        } else {
            // Por defecto: brew_sessions
            sb.append("id,recipe_id,coffee_lot_id,roast_batch_id,status,planned_method,planned_dose_g,planned_water_g,planned_temperature_c,planned_grind_setting,actual_dose_g,actual_water_g,actual_temperature_c,actual_grind_setting,total_time_seconds,final_beverage_weight_g,is_demo\r\n");
            for (BrewSession s : brewSessionRepository.findAllByUser(userId)) {
                row(sb, s.getId(), s.getRecipeId(), s.getCoffeeLotId(), s.getRoastBatchId(),
                        s.getStatus(), s.getPlannedMethod(), s.getPlannedDoseG(), s.getPlannedWaterG(),
                        s.getPlannedTemperatureC(), s.getPlannedGrindSetting(),
                        s.getActualDoseG(), s.getActualWaterG(), s.getActualTemperatureC(),
                        s.getActualGrindSetting(), s.getTotalTimeSeconds(), s.getFinalBeverageWeightG(),
                        Boolean.valueOf(s.isDemo()));
            }
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object c = cells[i];
            if (c != null) {
                String s = String.valueOf(c);
                if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
                    sb.append('"').append(s.replace("\"", "\"\"")).append('"');
                } else {
                    sb.append(s);
                }
            }
        }
        sb.append("\r\n");
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
