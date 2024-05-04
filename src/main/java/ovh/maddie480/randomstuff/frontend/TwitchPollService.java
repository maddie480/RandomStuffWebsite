package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This service is tied to the LNJ Twitch bot:
 * https://github.com/maddie480/RandomBackendStuff/blob/main/src/main/java/ovh/maddie480/randomstuff/backend/twitch/LNJTwitchBot.java
 * It displays poll results in real time (at least, it refreshes every 5 seconds), using a file dropped in /shared/temp by the backend
 * (that handles the real polling and communication with Twitch).
 */
@WebServlet(name = "TwitchPollService", urlPatterns = {"/twitch-poll", "twitch-poll.json"})
@MultipartConfig
public class TwitchPollService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Path pollFile = Paths.get("/shared/lnj-poll.json");

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
            try (BufferedReader br = Files.newBufferedReader(pollFile)) {
                pollInfo = new JSONObject(new JSONTokener(br));
            }

            request.setAttribute("title", pollInfo.getString("name"));
            request.setAttribute("answers", pollInfo.getJSONObject("answersWithCase"));

            PageRenderer.render(request, response, "twitch-poll", "LNJ – Sondage Twitch",
                    "Les résultats du dernier sondage LNJ sur Twitch.");
        }
    }
}
