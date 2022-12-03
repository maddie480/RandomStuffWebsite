package com.max480.randomstuff.gae;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A weird service allowing to copy-paste a Party Parrot in one click.
 */
@WebServlet(name = "ParrotQuickImporterService", urlPatterns = {"/parrot-quick-importer-online"})
public class ParrotQuickImporterService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("ParrotQuickImporterService");

    private static final Map<String, String> EXTRA_PARROTS = new LinkedHashMap<>();

    static {
        EXTRA_PARROTS.put("OSEF Parrot", "https://max480-random-stuff.appspot.com/img/osef_parrot.gif");
        EXTRA_PARROTS.put("AH Parrot", "https://max480-random-stuff.appspot.com/img/ah_parrot.gif");
        EXTRA_PARROTS.put("Flan Parrot", "https://max480-random-stuff.appspot.com/img/flan_parrot.gif");
        EXTRA_PARROTS.put("Zscaler Parrot", "https://max480-random-stuff.appspot.com/img/zscaler_parrot.gif");
        EXTRA_PARROTS.put("GitLab Parrot", "https://max480-random-stuff.appspot.com/img/gitlab_parrot.gif");
        EXTRA_PARROTS.put("Ember Parrot", "https://max480-random-stuff.appspot.com/img/ember_parrot.gif");
    }

    public static Map<String, String> getParrots() throws IOException {
        try {
            Map<String, String> parrots = new LinkedHashMap<>();

            for (Element elt : Jsoup.connect("https://cultofthepartyparrot.com/").get().select("article li img")) {
                parrots.put(elt.attr("alt"), "https://cultofthepartyparrot.com" + elt.attr("data-src"));
            }
            parrots.putAll(EXTRA_PARROTS);
            logger.log(Level.INFO, "There are " + parrots.size() + " parrots in my database :parrot_parrot:");
            return parrots;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Parrot retrieval failed: " + e.toString());
            throw e;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("parrots", getParrots());
        request.getRequestDispatcher("/WEB-INF/parrot-quick-importer.jsp").forward(request, response);
    }
}
