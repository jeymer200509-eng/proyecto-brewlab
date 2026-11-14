package com.brewlab.app;

import com.brewlab.data.db.DataSourceFactory;
import com.brewlab.data.db.SchemaMigrator;
import com.brewlab.data.repositories.CoffeeLotRepository;
import com.brewlab.data.repositories.EquipmentRepository;
import com.brewlab.data.repositories.RecipeRepository;
import com.brewlab.data.repositories.RoastBatchRepository;
import com.brewlab.data.repositories.UserRepository;
import com.brewlab.data.repositories.UserSettingsRepository;
import com.brewlab.data.repositories.WaterProfileRepository;
import com.brewlab.security.JwtService;
import com.brewlab.security.PasswordHasher;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.time.Clock;

/** Contenedor de servicios e infraestructura de la aplicación. */
public final class AppContainer implements AutoCloseable {
    public static final String CONTEXT_ATTR = "brewlab.container";

    private final AppConfig config;
    private final DataSourceFactory.ManagedDataSource managedDataSource;
    private final int schemaVersion;
    private final Clock clock;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final CoffeeLotRepository coffeeLotRepository;
    private final RoastBatchRepository roastBatchRepository;
    private final EquipmentRepository equipmentRepository;
    private final WaterProfileRepository waterProfileRepository;
    private final RecipeRepository recipeRepository;

    public AppContainer(AppConfig config, DataSourceFactory.ManagedDataSource managedDataSource, Clock clock) {
        this.config = config;
        this.managedDataSource = managedDataSource;
        DataSource ds = managedDataSource.getDataSource();
        SchemaMigrator migrator = new SchemaMigrator(ds);
        migrator.migrate();
        this.schemaVersion = migrator.currentVersion();
        this.clock = clock;
        this.passwordHasher = new PasswordHasher();
        this.jwtService = new JwtService(config.getJwtSecretBase64(), config.getJwtTtlMinutes());
        this.userRepository = new UserRepository(ds);
        this.userSettingsRepository = new UserSettingsRepository(ds);
        this.coffeeLotRepository = new CoffeeLotRepository(ds);
        this.roastBatchRepository = new RoastBatchRepository(ds);
        this.equipmentRepository = new EquipmentRepository(ds);
        this.waterProfileRepository = new WaterProfileRepository(ds);
        this.recipeRepository = new RecipeRepository(ds);
    }

    public static AppContainer from(ServletContext ctx) {
        AppContainer c = (AppContainer) ctx.getAttribute(CONTEXT_ATTR);
        if (c == null) {
            throw new IllegalStateException("AppContainer no inicializado en ServletContext");
        }
        return c;
    }

    public AppConfig getConfig() {
        return config;
    }

    public DataSource getDataSource() {
        return managedDataSource.getDataSource();
    }

    public DataSourceFactory.ManagedDataSource getManagedDataSource() {
        return managedDataSource;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Clock getClock() {
        return clock;
    }

    public PasswordHasher getPasswordHasher() {
        return passwordHasher;
    }

    public JwtService getJwtService() {
        return jwtService;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public UserSettingsRepository getUserSettingsRepository() {
        return userSettingsRepository;
    }

    public CoffeeLotRepository getCoffeeLotRepository() {
        return coffeeLotRepository;
    }

    public RoastBatchRepository getRoastBatchRepository() {
        return roastBatchRepository;
    }

    public EquipmentRepository getEquipmentRepository() {
        return equipmentRepository;
    }

    public WaterProfileRepository getWaterProfileRepository() {
        return waterProfileRepository;
    }

    public RecipeRepository getRecipeRepository() {
        return recipeRepository;
    }

    @Override
    public void close() {
        managedDataSource.close();
    }
}
