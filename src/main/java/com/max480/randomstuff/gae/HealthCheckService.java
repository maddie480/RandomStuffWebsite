package com.max480.randomstuff.gae;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A simple service that just responds with "yes the app exists", for use with availability checks.
 */
@WebServlet(name = "HealthCheckService", urlPatterns = {"/healthcheck"})
public class HealthCheckService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/plain");
        response.getWriter().write("The service is up and running!");
    }
}
