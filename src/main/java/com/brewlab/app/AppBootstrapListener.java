package com.brewlab.app;

import com.brewlab.data.db.DataSourceFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.time.Clock;

/** Inicializa configuración, migraciones de base de datos y servicios al arrancar Tomcat. */
public final class AppBootstrapListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        String realPath = ctx.getRealPath("/");
        File webRoot = realPath != null ? new File(realPath) : new File(".");
        AppConfig config = AppConfig.load(webRoot);
        DataSourceFactory.ManagedDataSource mds = DataSourceFactory.create(config);
        Clock clock = Clock.system(config.getTimezone());
        AppContainer container = new AppContainer(config, mds, clock);
        ctx.setAttribute(AppContainer.CONTEXT_ATTR, container);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        Object attr = ctx.getAttribute(AppContainer.CONTEXT_ATTR);
        if (attr instanceof AppContainer) {
            ((AppContainer) attr).close();
        }
    }
}
