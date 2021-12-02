package com.max480.randomstuff.gae;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The default servlet catching all pages that didn't match any other route
 * (because yes, that's what "/" does... it isn't just the server root.)
 */
@WebServlet(name = "RouteNotFound", urlPatterns = {"/"})
public class RouteNotFoundServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger("RouteNotFoundServlet");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/")) {
            // redirect to home
            response.setStatus(302);
            response.setHeader("Location", "/home.html");
        } else {
            // display a simple 404 page
            response.setStatus(404);
            response.setHeader("Content-Type", "text/html; charset=UTF-8");
            response.getWriter().write("<html>" +
                    "<link rel=\"stylesheet\" href=\"/css/common.css\">" +
                    "<link rel=\"stylesheet\" href=\"/css/page-not-found.css\">" +
                    "<h1>\u274C Not Found</h1><a href=\"/\">\u2B05 Back to Home Page</a></html>");
            logger.warning("Route not found!");
        }
    }
}
