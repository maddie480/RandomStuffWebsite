package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * A servlet serving two assets for my GameBanana profile: a CSS file coming from Cloud Storage, and a font that has
 * to be hosted here to have a proper Access-Control-Allow-Origin header for use on gamebanana.com.
 */
@WebServlet(name = "GameBananaAssetsServlet", urlPatterns = {"/fonts/Renogare.otf", "/css/gamebanana-profile-background.css"})
public class GameBananaAssetsServlet extends HttpServlet {
    private final Logger logger = Logger.getLogger("GameBananaAssetsServlet");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().equals("/fonts/Renogare.otf")) {
            // this is a static asset, but we need to serve it with special response headers to be able to use it on gamebanana.com.
            resp.setContentType("application/font-sfnt");
            resp.setHeader("Access-Control-Allow-Origin", "https://gamebanana.com");
            try (InputStream is = GameBananaAssetsServlet.class.getClassLoader().getResourceAsStream("font-generator/fonts/Renogare.otf")) {
                IOUtils.copy(is, resp.getOutputStream());
            }

        } else if (req.getRequestURI().equals("/css/gamebanana-profile-background.css")) {
            // serve it from Cloud Storage
            resp.setHeader("Content-Type", "text/css");
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("gamebanana-profile-background.css")) {
                IOUtils.copy(is, resp.getOutputStream());
            }

        } else {
            logger.warning("Not found");
            resp.setStatus(404);
        }
    }
}