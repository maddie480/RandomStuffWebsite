package com.max480.randomstuff.gae;

import javax.servlet.ServletException;
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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/")) {
            PageRenderer.render(request, response, "home", "max480's Random Stuff",
                    "A website with a bunch of Celeste tools and Discord bots.");
        } else {
            // display a simple 404 page
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
            logger.warning("Route not found!");
        }
    }
}
