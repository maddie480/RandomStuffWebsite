package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service is tied to the LNJ Twitch bot:
 * https://github.com/maddie480/RandomBackendStuff/blob/main/src/main/java/com/max480/randomstuff/backend/twitch/LNJTwitchBot.java
 * It displays poll results in real time (at least, it refreshes every 5 seconds), using a file dropped in /shared/temp by the backend
 * (that handles the real polling and communication with Twitch).
 */
@WebServlet(name = "TwitchPollService", urlPatterns = {"/twitch-polls/*"})
@MultipartConfig
public class TwitchPollService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TwitchPollService.class);

    private final Pattern pageUrlPattern = Pattern.compile("^/twitch-polls/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(?:\\.json)?/?$");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Matcher pageUrlMatch = pageUrlPattern.matcher(request.getRequestURI());

        if (!pageUrlMatch.matches()) {
            log.warn("Invalid URL format!");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
            return;
        }

        String uuid = pageUrlMatch.group(1);
        Path pollFile = Paths.get("/shared/temp/lnj-polls/" + uuid + ".json");

        if (!Files.exists(pollFile)) {
            log.warn("Poll does not exist!");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
            return;
        }

        if (request.getRequestURI().endsWith(".json")) {
            // literally send out the raw JSON file
            response.setContentType("application/json");

            try (InputStream is = Files.newInputStream(pollFile);
                 OutputStream os = response.getOutputStream()) {

                IOUtils.copy(is, os);
            }
        } else {
            // display it as a page
            JSONObject pollInfo;
            try (InputStream is = Files.newInputStream(pollFile)) {
                pollInfo = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            }

            List<String> answers = new ArrayList<>(pollInfo.getJSONObject("answers").keySet());
            answers.sort(Comparator.comparing(String::toLowerCase));

            request.setAttribute("uuid", uuid);
            request.setAttribute("title", pollInfo.getString("title"));
            request.setAttribute("answers", answers);

            PageRenderer.render(request, response, "twitch-poll", "Sondage Twitch – \"" + pollInfo.getString("title") + "\"",
                    "Les résultats du sondage \"" + pollInfo.getString("title") + "\" sur Twitch. " +
                            "Le lien restera valide pendant 24 heures après le début du sondage.");
        }
    }
}
