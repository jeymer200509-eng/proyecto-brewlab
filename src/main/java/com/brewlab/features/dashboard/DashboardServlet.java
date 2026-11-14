package com.brewlab.features.dashboard;

import com.brewlab.app.AppContainer;
import com.brewlab.domain.calc.BrewRatio;
import com.brewlab.domain.format.Formats;
import com.brewlab.domain.model.User;
import com.brewlab.domain.model.UserSettings;
import com.brewlab.web.WebSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

/** Pantalla principal de trabajo (Dashboard del laboratorio). */
public final class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        AppContainer container = AppContainer.from(getServletContext());
        User user = WebSupport.currentUser(req);
        UserSettings settings = container.getUserSettingsRepository().findOrDefault(user.getId());

        req.setAttribute("settings", settings);
        req.setAttribute("todayFormatted", Formats.date(LocalDate.now(container.getClock())));
        req.setAttribute("sampleRatioFormula", BrewRatio.FORMULA);
        req.setAttribute("dbEngineLabel", container.getManagedDataSource().getEngineLabel());
        req.setAttribute("schemaVersion", Integer.valueOf(container.getSchemaVersion()));

        WebSupport.render(req, resp, "inicio", "Panel de Laboratorio — BrewLab", "dashboard/index.jsp");
    }
}
