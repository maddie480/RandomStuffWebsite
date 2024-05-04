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
 * APIs that provide utility stuff, for use as Mattermost slash commands.
 * Those require keys to use, and are mostly only open-sourced because the entire website is,
 * which allows for continuous deployment to be set up.
 * <p>
 * Lock/unlock is a simple resource locking system, exploit and absents do lookups on an iCalendar,
 * and consistencycheck compares a calendar in an internal JSON format with the iCalendar one.
 */
@WebServlet(name = "MattermostService", urlPatterns = {
        "/mattermost/exploit", "/mattermost/absents", "/mattermost/lock", "/mattermost/unlock",
        "/mattermost/consistencycheck"})
public class MattermostService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MattermostService.class);


    // those are the resources that can be locked or unlocked through /lock and /unlock
    private final List<String> resources = Arrays.asList("integ1", "integ2");

    public static PlanningExploit retrievePlanningExploit() throws IOException {
        PlanningExploit exploit;

        // that planning is parsed from an iCalendar and serialized by the backend, because that can take a bit of time
        // (and the iCalendar provider can turn out to be quite unstable).
        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(Paths.get("/shared/mattermost/planning_exploit.ser")))) {
            exploit = (PlanningExploit) is.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }

        log.info("Exploit entries count: {}", exploit.principalExploit.size());
        log.info("Holiday entries count: {}", exploit.holidays.values().stream().mapToInt(List::size).sum());

        return exploit;
    }

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
            case "/mattermost/exploit" -> {
                if (!SecretConstants.MATTERMOST_TOKEN_EXPLOIT.equals(request.getParameter("token"))) {
                    log.warn("A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(commandExploit());
            }
            case "/mattermost/consistencycheck" -> {
                if (!("token=" + SecretConstants.MATTERMOST_TOKEN_CONSISTENCYCHECK).equals(request.getQueryString())) {
                    log.warn("A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String responseString = commandConsistencyCheck(IOUtils.toString(request.getInputStream(), UTF_8));
                response.getWriter().write(responseString);
            }
            case "/mattermost/absents" -> {
                if (!SecretConstants.MATTERMOST_TOKEN_ABSENTS.equals(request.getParameter("token"))) {
                    log.warn("A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(commandAbsents());
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

    private String commandExploit() {
        PlanningExploit exploit;
        try {
            exploit = retrievePlanningExploit();
        } catch (IOException e) {
            log.error("Problem while getting exploit planning", e);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la récupération du planning d'exploit a échoué. :ckc:");
            return jsonObject.toString();
        }

        JSONObject jsonObject = new JSONObject();

        StringBuilder messageBuilder = new StringBuilder("**Planning d'exploit**\nActuellement : **");
        messageBuilder.append(formatName(exploit.principalExploit.get(0))).append("**, backup **")
                .append(formatName(exploit.backupExploit.get(0))).append("**");

        for (int i = 0; i < exploit.exploitTimes.size(); i++) {
            messageBuilder.append("\n").append(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.FRANCE).format(exploit.exploitTimes.get(i)))
                    .append(" : **").append(formatName(exploit.principalExploit.get(i + 1))).append("**, backup **")
                    .append(formatName(exploit.backupExploit.get(i + 1))).append("**");
        }

        jsonObject.put("response_type", "in_channel");
        jsonObject.put("text", messageBuilder.toString());
        return jsonObject.toString();
    }

    private String commandAbsents() {
        PlanningExploit exploit;
        try {
            exploit = retrievePlanningExploit();
        } catch (IOException e) {
            log.error("Problem while getting exploit planning", e);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la récupération du planning d'exploit a échoué. :ckc:");
            return jsonObject.toString();
        }

        Set<String> absents = new HashSet<>();
        for (Map.Entry<String, List<Pair<ZonedDateTime, ZonedDateTime>>> entry : exploit.holidays.entrySet()) {
            for (Pair<ZonedDateTime, ZonedDateTime> holiday : entry.getValue()) {
                if (holiday.getLeft().isBefore(ZonedDateTime.now()) && holiday.getRight().isAfter(ZonedDateTime.now())) {
                    // person currently in holidays
                    absents.add(formatName(entry.getKey()));
                    break;
                }
            }
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response_type", "in_channel");
        jsonObject.put("text", absents.isEmpty() ? "Personne n'est absent actuellement !" :
                "**" + joinAll(absents) + (absents.size() == 1 ? "** est actuellement absent(e)." : "** sont actuellement absents."));
        return jsonObject.toString();
    }

    private String joinAll(Collection<String> strings) {
        long left = strings.size();
        StringBuilder b = new StringBuilder();
        for (String string : strings) {
            left--;
            b.append(string).append(left == 0 ? "" : (left == 1 ? "** et **" : "**, **"));
        }
        return b.toString();
    }

    private String formatName(String name) {
        return StringUtils.capitalize(name.substring(0, name.indexOf(" ")).toLowerCase(Locale.ROOT)) + name.substring(name.indexOf(" "));
    }

    // this method compares the calendar used by /absents and one in a custom internal JSON format from another tool.
    // open-sourcing this has little interest, but...
    private String commandConsistencyCheck(String calendarString) {
        PlanningExploit exploit;
        try {
            exploit = retrievePlanningExploit();
        } catch (IOException e) {
            log.error("Problem while getting exploit planning", e);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la récupération du planning d'exploit a échoué. :ckc:");
            return jsonObject.toString();
        }

        List<String> issues = new ArrayList<>();

        JSONObject calendar = new JSONObject(calendarString);

        for (String userId : calendar.getJSONObject("users").keySet()) {
            String userName = calendar.getJSONObject("users").getJSONObject(userId).getString("name");

            // Nom Prénom => PRENOM NOM
            userName = (userName.substring(userName.lastIndexOf(" ") + 1) + " "
                    + userName.substring(0, userName.lastIndexOf(" "))).toUpperCase(Locale.ROOT);

            for (String month : calendar.getJSONObject("dates").keySet()) {
                final JSONObject monthUserObject = calendar.getJSONObject("dates").getJSONObject(month).getJSONObject("users").getJSONObject(userId);
                for (String day : monthUserObject.keySet()) {

                    final JSONObject dayUserObject = monthUserObject.getJSONObject(day);
                    LocalDate date = LocalDate.parse(day);

                    if (date.isBefore(LocalDate.now())) continue;

                    for (Object o : dayUserObject.getJSONArray("AM")) {
                        if ("absent".equals(o) && (!exploit.holidays.containsKey(userName) || exploit.holidays.get(userName).stream().noneMatch(holiday ->
                                holiday.getLeft().isBefore(date.atTime(8, 1, 0).atZone(ZoneId.of("Europe/Paris"))) &&
                                        holiday.getRight().isAfter(date.atTime(11, 59, 0).atZone(ZoneId.of("Europe/Paris")))))) {

                            // someone is declared absent in the tool but not in /absents (morning)
                            issues.add("**" + formatName(userName) + "** le " + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.FRANCE)
                                    .format(date) + " matin");
                        }
                    }
                    for (Object o : dayUserObject.getJSONArray("PM")) {
                        if ("absent".equals(o) && (!exploit.holidays.containsKey(userName) || exploit.holidays.get(userName).stream().noneMatch(holiday ->
                                holiday.getLeft().isBefore(date.atTime(14, 1, 0).atZone(ZoneId.of("Europe/Paris"))) &&
                                        holiday.getRight().isAfter(date.atTime(17, 59, 0).atZone(ZoneId.of("Europe/Paris")))))) {

                            // someone is declared absent in the tool but not in /absents (afternoon)
                            issues.add("**" + formatName(userName) + "** le " + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.FRANCE)
                                    .format(date) + " après-midi");
                        }
                    }
                }
            }
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response_type", "in_channel");
        jsonObject.put("text", issues.isEmpty() ? ":white_check_mark: Aucune désynchro Confluence n'a été détectée pour les congés." :
                ":x: Il y a des désynchros Confluence pour les congés ! Ces congés ont été posés mais ne sont pas sur Confluence :\n- " + String.join("\n- ", issues));
        return jsonObject.toString();
    }
}
