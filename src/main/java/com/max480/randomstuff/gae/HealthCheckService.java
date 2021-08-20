package com.max480.randomstuff.gae;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A simple service that just responds with "yes the app exists", for use with availability checks.
 */
@WebServlet(name = "HealthCheckService", urlPatterns = {"/healthcheck", "/bot-healthcheck", "/bot-says-hi"})
public class HealthCheckService extends HttpServlet {
    private long lastBotSign = System.currentTimeMillis();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/healthcheck")) {
            // server healthcheck
            response.setHeader("Content-Type", "text/plain");
            response.getWriter().write("The service is up and running!");
        } else if (request.getRequestURI().equals("/bot-says-hi")
                && ("key=" + Constants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {

            // bot said hi!
            lastBotSign = System.currentTimeMillis();

        } else if (request.getRequestURI().equals("/bot-healthcheck")) {
            response.setHeader("Content-Type", "text/plain");
            if (System.currentTimeMillis() - lastBotSign > 600_000) {
                // bot last said hi more than 10 minutes ago, this is fishy.
                response.getWriter().write("KO");
            } else {
                // all good!
                response.getWriter().write("OK");
            }
        } else {
            response.setStatus(404);
        }
    }
}
