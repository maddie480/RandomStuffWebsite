package com.max480.randomstuff.gae;

import com.google.appengine.api.datastore.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.max480.randomstuff.gae.ConnectionUtils.openStreamWithTimeout;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * APIs that provide utility stuff, for use as Mattermost slash commands.
 * Those require keys to use, and are mostly only open-sourced because the entire website is,
 * which allows for continuous deployment to be set up.
 * <p>
 * Lock/unlock is a simple resource locking system, exploit and absents do lookups on an iCalendar,
 * and consistencycheck compares a calendar in an internal JSON format with the iCalendar one.
 */
@WebServlet(name = "MattermostService", loadOnStartup = 3, urlPatterns = {
        "/mattermost/exploit", "/mattermost/absents", "/mattermost/lock", "/mattermost/unlock",
        "/mattermost/planning-reload", "/mattermost/consistencycheck"})
public class MattermostService extends HttpServlet {

    private final Logger logger = Logger.getLogger("MattermostService");
    private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // those are the resources that can be locked or unlocked through /lock and /unlock
    private final List<String> resources = Arrays.asList("integ1", "integ2");

    private PlanningExploit cachedPlanningExploit;

    @Override
    public void init() {
        try {
            cachedPlanningExploit = getExploit();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Preloading planning failed: " + e.toString());
        }
    }

    private static class PlanningExploit implements Serializable {
        private static final long serialVersionUID = 56185131613831863L;

        private final List<ZonedDateTime> exploitTimes = new ArrayList<>();
        private final List<String> principalExploit = new ArrayList<>();
        private final List<String> backupExploit = new ArrayList<>();

        private final Map<String, List<Pair<ZonedDateTime, ZonedDateTime>>> holidays = new HashMap<>();
    }

    private PlanningExploit getExploit() throws IOException {
        PlanningExploit exploit = new PlanningExploit();

        // the "exploit" planning is stored in iCalendar format and has someone as "principal" and one as "backup" every week.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openStreamWithTimeout(new URL(SecretConstants.EXPLOIT_PLANNING_URL))))) {
            // date => name
            TreeMap<String, String> principals = new TreeMap<>();
            TreeMap<String, String> backups = new TreeMap<>();

            // jank iCal parser incoming!
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("BEGIN:VEVENT")) {
                    Map<String, String> properties = new HashMap<>();
                    String propertyBeingParsed = null;
                    String propertyValue = null;

                    // get event fields until we hit the end of it
                    while (!(line = reader.readLine()).equals("END:VEVENT")) {
                        if (propertyBeingParsed == null || !line.startsWith(" ")) {
                            // start of another property
                            if (propertyBeingParsed != null) {
                                properties.put(propertyBeingParsed, propertyValue);
                            }

                            // [name]:[value] or [name];[value]
                            if (line.contains(":")) {
                                propertyBeingParsed = line.substring(0, line.indexOf(":"));
                                propertyValue = line.substring(line.indexOf(":") + 1);
                            } else {
                                propertyBeingParsed = line.substring(0, line.indexOf(";"));
                                propertyValue = line.substring(line.indexOf(";") + 1);
                            }
                        } else {
                            // lines starting with a space are a continuation of the previous one.
                            propertyValue += line.substring(1);
                        }
                    }

                    // add the last property of the event
                    if (propertyBeingParsed != null) {
                        properties.put(propertyBeingParsed, propertyValue);
                    }

                    // is this a valid exploit entry?
                    if ("Exploitation".equals(properties.get("CATEGORIES")) && Arrays.asList("Principal", "Backup").contains(properties.get("SUMMARY"))
                            && properties.containsKey("DTSTART;VALUE=DATE") && properties.getOrDefault("ATTENDEE", "").contains("CN=")) {

                        // extract user
                        String userName = properties.get("ATTENDEE");
                        userName = userName.substring(userName.indexOf("CN=") + 3);
                        userName = userName.substring(0, userName.indexOf(";"));

                        // put it in the appropriate map
                        if (properties.get("SUMMARY").equals("Principal")) {
                            principals.put(properties.get("DTSTART;VALUE=DATE"), userName);
                        } else {
                            backups.put(properties.get("DTSTART;VALUE=DATE"), userName);
                        }
                    }

                    // is this a valid holidays entry?
                    if (Arrays.asList("Formation École", "leaves", "Formation").contains(properties.get("CATEGORIES"))
                            && (properties.containsKey("DTSTART;VALUE=DATE") || properties.containsKey("DTSTART;TZID=Europe/Paris"))
                            && (properties.containsKey("DTEND;VALUE=DATE") || properties.containsKey("DTEND;TZID=Europe/Paris"))
                            && properties.getOrDefault("ATTENDEE", "").contains("CN=")) {

                        // get the user
                        String userName = properties.get("ATTENDEE");
                        userName = userName.substring(userName.indexOf("CN=") + 3);
                        userName = userName.substring(0, userName.indexOf(";"));

                        // parse the date (that can contain a time or not) and put it in the planning
                        List<Pair<ZonedDateTime, ZonedDateTime>> userHolidays = exploit.holidays.getOrDefault(userName, new ArrayList<>());
                        userHolidays.add(new ImmutablePair<>(
                                properties.containsKey("DTSTART;VALUE=DATE") ?
                                        LocalDate.parse(properties.get("DTSTART;VALUE=DATE"), DateTimeFormatter.ofPattern("yyyyMMdd"))
                                                .atTime(0, 0, 0).atZone(ZoneId.of("Europe/Paris")) :
                                        LocalDateTime.parse(properties.get("DTSTART;TZID=Europe/Paris"), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                                                .atZone(ZoneId.of("Europe/Paris")),
                                properties.containsKey("DTEND;VALUE=DATE") ?
                                        LocalDate.parse(properties.get("DTEND;VALUE=DATE"), DateTimeFormatter.ofPattern("yyyyMMdd"))
                                                .atTime(0, 0, 0).atZone(ZoneId.of("Europe/Paris")) :
                                        LocalDateTime.parse(properties.get("DTEND;TZID=Europe/Paris"), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
                                                .atZone(ZoneId.of("Europe/Paris"))));
                        exploit.holidays.put(userName, userHolidays);
                    }
                }
            }

            for (String date : principals.keySet()) {
                if (backups.containsKey(date)) {
                    // if we have a principal/backup pair, add it to the planning, starting on Monday 8am
                    exploit.principalExploit.add(principals.get(date));
                    exploit.backupExploit.add(backups.get(date));
                    exploit.exploitTimes.add(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"))
                            .atTime(8, 0).atZone(ZoneId.of("Europe/Paris")));
                }
            }

            // remove past exploit entries
            exploit.exploitTimes.remove(0);
            while (!exploit.exploitTimes.isEmpty() && exploit.exploitTimes.get(0).isBefore(ZonedDateTime.now())) {
                exploit.exploitTimes.remove(0);
                exploit.principalExploit.remove(0);
                exploit.backupExploit.remove(0);
            }

            // cleanup past holidays
            for (List<Pair<ZonedDateTime, ZonedDateTime>> holidays : exploit.holidays.values()) {
                holidays.removeIf(holiday -> holiday.getRight().isBefore(ZonedDateTime.now()));
            }

            logger.info("Exploit entries count: " + exploit.principalExploit.size());
            logger.info("Holiday entries count: " + exploit.holidays.values().stream().mapToInt(List::size).sum());

            return exploit;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/mattermost/planning-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                cachedPlanningExploit = getExploit();
            } else {
                logger.warning("Invalid key");
                response.setStatus(403);
            }
        } else {
            logger.warning("Route not found");
            response.setStatus(404);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getRequestURI()) {
            case "/mattermost/lock": {
                if (!SecretConstants.MATTERMOST_TOKEN_LOCK.equals(request.getParameter("token"))) {
                    logger.log(Level.WARNING, "A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String target = request.getParameter("text");
                String userId = request.getParameter("user_name");

                String responseString = commandLock(userId, target);
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/unlock": {
                if (!SecretConstants.MATTERMOST_TOKEN_UNLOCK.equals(request.getParameter("token"))) {
                    logger.log(Level.WARNING, "A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String target = request.getParameter("text");
                String userId = request.getParameter("user_name");

                String responseString = commandUnlock(userId, target);
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/exploit": {
                if (!SecretConstants.MATTERMOST_TOKEN_EXPLOIT.equals(request.getParameter("token"))) {
                    logger.log(Level.WARNING, "A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(commandExploit());
                break;
            }
            case "/mattermost/consistencycheck": {
                if (!("token=" + SecretConstants.MATTERMOST_TOKEN_CONSISTENCYCHECK).equals(request.getQueryString())) {
                    logger.log(Level.WARNING, "A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String responseString = commandConsistencyCheck(IOUtils.toString(request.getInputStream(), UTF_8));
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/absents": {
                if (!SecretConstants.MATTERMOST_TOKEN_ABSENTS.equals(request.getParameter("token"))) {
                    logger.log(Level.WARNING, "A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(commandAbsents());
                break;
            }
            default: {
                logger.warning("Route not found");
                response.setStatus(404);
                break;
            }
        }
    }

    private String commandLock(String user, String resource) {
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
            Entity datastoreThing = new Entity("ressources", resource);
            datastoreThing.setProperty("lockedBy", user);
            datastoreThing.setProperty("lockTime", System.currentTimeMillis());
            datastore.put(datastoreThing);

            jsonObject.put("text", ":lock: @" + user + " a verrouillé la ressource **" + resource + "**.");
        }

        return jsonObject.toString();
    }

    private String commandUnlock(String user, String resource) {
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
                datastore.delete(KeyFactory.createKey("ressources", resource));
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

    private String figureOutWhoLocked(String resource) {
        String lockedBy = null;
        try {
            Entity datastoreEntity = datastore.get(KeyFactory.createKey("ressources", resource));

            // locks expire after 12 hours.
            if ((long) datastoreEntity.getProperty("lockTime") > System.currentTimeMillis() - (12 * 3600 * 1000)) {
                lockedBy = (String) datastoreEntity.getProperty("lockedBy");
            }
        } catch (EntityNotFoundException e) {
            // oops, in that case lockedBy will stay null.
        }
        return lockedBy;
    }

    private String commandExploit() {
        PlanningExploit exploit;
        try {
            exploit = cachedPlanningExploit == null ? getExploit() : cachedPlanningExploit;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while getting exploit planning: " + e.toString());

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
            exploit = cachedPlanningExploit == null ? getExploit() : cachedPlanningExploit;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while getting exploit planning: " + e.toString());

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
        long left = strings.stream().count();
        StringBuilder b = new StringBuilder("");
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
            exploit = cachedPlanningExploit == null ? getExploit() : cachedPlanningExploit;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while getting exploit planning: " + e.toString());

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
