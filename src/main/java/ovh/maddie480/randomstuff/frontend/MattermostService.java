package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static com.max480.discord.slashcommandbot.SlashCommandBot.PlanningExploit;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Private APIs that provide a simple resource locking system, through /lock and /unlock commands.
 */
@WebServlet(name = "MattermostService", urlPatterns = {"/mattermost/lock", "/mattermost/unlock"})
public class MattermostService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MattermostService.class);

    // those are the resources that can be locked or unlocked through /lock and /unlock
    private final List<String> resources = Arrays.asList("integ1", "integ2");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getRequestURI()) {
            case "/mattermost/lock" -> {
                if (!SecretConstants.MATTERMOST_TOKEN_LOCK.equals(request.getParameter("token"))) {
                    log.warn("A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String target = request.getParameter("text");
                String userId = request.getParameter("user_name");

                String responseString = commandLock(userId, target);
                response.getWriter().write(responseString);
            }
            case "/mattermost/unlock" -> {
                if (!SecretConstants.MATTERMOST_TOKEN_UNLOCK.equals(request.getParameter("token"))) {
                    log.warn("A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String target = request.getParameter("text");
                String userId = request.getParameter("user_name");

                String responseString = commandUnlock(userId, target);
                response.getWriter().write(responseString);
            }
            default -> {
                log.warn("Route not found");
                response.setStatus(404);
            }
        }
    }

    private String commandLock(String user, String resource) throws IOException {
        if (!resources.contains(resource)) {
            // trying to lock non-existent resource
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "La ressource que tu as demandée n'existe pas ! Les ressources valides sont : " + String.join(", ", resources));
            return jsonObject.toString();
        }

        String lockedBy = figureOutWhoLocked(resource);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response_type", "in_channel");

        if (lockedBy != null) {
            if (lockedBy.equals(user)) {
                // user has locked resource
                jsonObject.put("text", ":x: Tu as déjà verrouillé la ressource **" + resource + "**. :thinking:");
            } else {
                // user tries to lock a resource already locked by someone else
                jsonObject.put("text", ":x: La ressource **" + resource + "** est actuellement verrouillée par @" + lockedBy + " ! :a:");
            }
        } else {
            // all good, lock the resource
            JSONObject data = new JSONObject();
            data.put("lockedBy", user);
            data.put("lockTime", System.currentTimeMillis());

            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("/shared/mattermost/lock_" + resource + ".json"))) {
                data.write(bw);
            }

            jsonObject.put("text", ":lock: @" + user + " a verrouillé la ressource **" + resource + "**.");
        }

        return jsonObject.toString();
    }

    private String commandUnlock(String user, String resource) throws IOException {
        if (!resources.contains(resource)) {
            // trying to unlock non-existent resource
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "La ressource que tu as demandée n'existe pas ! Les ressources valides sont : " + String.join(", ", resources));
            return jsonObject.toString();
        }

        String lockedBy = figureOutWhoLocked(resource);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response_type", "in_channel");

        if (lockedBy != null) {
            if (lockedBy.equals(user)) {
                // all good, unlock the resource
                Files.delete(Paths.get("/shared/mattermost/lock_" + resource + ".json"));
                jsonObject.put("text", ":unlock: @" + user + " a déverrouillé la ressource **" + resource + "**.");
            } else {
                // resource is locked... by someone else
                jsonObject.put("text", ":x: La ressource **" + resource + "** est verrouillée par @" + lockedBy + ", tu ne peux pas la déverrouiller à sa place ! :a:");
            }
        } else {
            // resource is not locked
            jsonObject.put("text", ":x: La ressource **" + resource + "** n'est pas verrouillée actuellement. :thinking:");
        }

        return jsonObject.toString();
    }

    private String figureOutWhoLocked(String resource) throws IOException {
        Path lockFile = Paths.get("/shared/mattermost/lock_" + resource + ".json");
        if (!Files.exists(lockFile)) {
            return null;
        }

        JSONObject lockData;
        try (BufferedReader br = Files.newBufferedReader(lockFile)) {
            lockData = new JSONObject(new JSONTokener(br));
        }

        // locks expire after 12 hours.
        if (lockData.getLong("lockTime") > System.currentTimeMillis() - (12 * 3600 * 1000)) {
            return lockData.getString("lockedBy");
        }
        return null;
    }
}
