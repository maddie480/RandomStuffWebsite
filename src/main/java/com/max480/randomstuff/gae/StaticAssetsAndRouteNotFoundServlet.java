package com.max480.randomstuff.gae;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The default servlet catching all pages that didn't match any other route
 * (because yes, that's what "/" does... it isn't just the server root.)
 * Returns static assets if they exist, or returns a "page not found" error if it does not exist.
 */
@WebServlet(name = "StaticAssetsAndRouteNotFound", urlPatterns = {"/"})
public class StaticAssetsAndRouteNotFoundServlet extends HttpServlet {
    private static final Map<String, String> CONTENT_TYPES = ImmutableMap.of(
            "json", "application/json",
            "ico", "image/x-icon",
            "css", "text/css",
            "png", "image/png",
            "js", "application/javascript",
            "otf", "font/otf",
            "html", "text/html",
            "svg", "image/svg+xml",
            "webm", "video/webm"
    );

    private static final Logger log = LoggerFactory.getLogger(StaticAssetsAndRouteNotFoundServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/")) {
            HomepageService.doGet(request, response);
        } else {
            String extension = request.getRequestURI().substring(request.getRequestURI().lastIndexOf(".") + 1);
            if (CONTENT_TYPES.containsKey(extension) && Stream.of("/celeste/", "/css/", "/fonts/", "/img/", "/js/", "/vids/", "/lua-cutscenes-documentation/")
                    .anyMatch(request.getRequestURI()::startsWith)) {

                try (InputStream is = StaticAssetsAndRouteNotFoundServlet.class.getClassLoader().getResourceAsStream("resources" + request.getRequestURI())) {
                    if (is != null) {
                        response.setContentType(CONTENT_TYPES.get(extension));
                        IOUtils.copy(is, response.getOutputStream());
                        return;
                    }
                }
            }

            // display a simple 404 page
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
            log.warn("Route not found: {}", request.getRequestURI());
        }
    }
}
