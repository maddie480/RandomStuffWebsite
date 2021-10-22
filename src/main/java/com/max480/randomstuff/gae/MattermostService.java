package com.max480.randomstuff.gae;

import com.google.appengine.api.datastore.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * APIs that provide utility and meme stuff, for use as Mattermost slash commands.
 * Some of them are public (in French though), and others require a key to use.
 * Most of those just pull info from some website and reformat it.
 */
@WebServlet(name = "MattermostService", loadOnStartup = 4, urlPatterns = {"/mattermost/tableflip", "/mattermost/bientotleweekend",
        "/mattermost/vacances", "/mattermost/chucknorris", "/mattermost/random", "/mattermost/toplyrics",
        "/mattermost/ckc", "/mattermost/jcvd", "/mattermost/languedebois", "/mattermost/noel", "/mattermost/joiesducode",
        "/mattermost/patoche", "/mattermost/coronavirus", "/mattermost/fakename", "/mattermost/tendancesyoutube", "/mattermost/putaclic",
        "/mattermost/exploit", "/mattermost/absents", "/mattermost/randomparrot", "/mattermost/monkeyuser", "/mattermost/xkcd",
        "/mattermost/lock", "/mattermost/unlock", "/mattermost/planning-reload", "/mattermost/consistencycheck", "/mattermost/infopipo",
        "/mattermost/weekend", "/mattermost/eddy", "/mattermost/pipo"})
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

    private static class PlanningExploit {
        private final List<ZonedDateTime> exploitTimes = new ArrayList<>();
        private final List<String> principalExploit = new ArrayList<>();
        private final List<String> backupExploit = new ArrayList<>();

        private final Map<String, List<Pair<ZonedDateTime, ZonedDateTime>>> holidays = new HashMap<>();
    }

    private PlanningExploit getExploit() throws IOException {
        PlanningExploit exploit = new PlanningExploit();

        // the "exploit" planning is stored in iCalendar format and has someone as "principal" and one as "backup" every week.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(SecretConstants.EXPLOIT_PLANNING_URL).openStream()))) {
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
        if (request.getRequestURI().equals("/mattermost/planning-reload")
                && ("key=" + SecretConstants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {

            cachedPlanningExploit = getExploit();
        } else {
            response.setStatus(404);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getRequestURI()) {
            case "/mattermost/tableflip":
                response.setHeader("Content-Type", "application/json");
                response.getWriter().write("{\"response_type\": \"in_channel\", \"text\": \"(\\u256f\\u00b0\\u25a1\\u00b0\\uff09\\u256f\\ufe35 \\u253b\\u2501\\u253b\"}");
                break;
            case "/mattermost/random": {
                response.setHeader("Content-Type", "application/json");

                String number = request.getParameter("text");
                String responseString = commandRandom(number);
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/vacances": {
                if (!Arrays.asList(SecretConstants.MATTERMOST_TOKEN_VACANCES.split(";")).contains(request.getParameter("token"))) {
                    logger.log(Level.WARNING, "A wrong token was given");
                    response.setStatus(403);
                    return;
                }

                response.setHeader("Content-Type", "application/json");

                String target = request.getParameter("text");
                String userId = request.getParameter("user_id");

                String responseString = commandVacances(target, userId);
                response.getWriter().write(responseString);
                break;
            }
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
            case "/mattermost/chucknorris": {
                response.setHeader("Content-Type", "application/json");

                String param = request.getParameter("text");

                String responseString = commandChuckNorris(param == null || !param.equals("all"));
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/toplyrics":
                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(commandTopLyrics(false));
                break;
            case "/mattermost/patoche":
                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(commandTopLyrics(true));
                break;
            case "/mattermost/ckc":
                response.setHeader("Content-Type", "application/json");
                response.getWriter().write("{\"response_type\": \"in_channel\", \"text\": \"https://max480-random-stuff.appspot.com/estcequeckc.html\"}");
                break;
            case "/mattermost/jcvd": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandJCVD();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/languedebois": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandLangueDeBois();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/bientotleweekend": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandBientotLeWeekend();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/noel": {
                response.setHeader("Content-Type", "application/json");

                String responseString = ChristmasFilmGenerator.generateStory();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/joiesducode": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandJoiesDuCode();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/coronavirus": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandCoronavirus();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/fakename": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandFakeName();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/putaclic": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandPutaclic();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/tendancesyoutube": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandTendancesYoutube();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/randomparrot": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandRandomParrot();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/monkeyuser": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandMonkeyUser();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/xkcd": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandXkcd();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/infopipo": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandInfoPipo();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/weekend": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandWeekend(request.getParameter("user_name"));
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/eddy": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandEddy();
                response.getWriter().write(responseString);
                break;
            }
            case "/mattermost/pipo": {
                response.setHeader("Content-Type", "application/json");

                String responseString = commandPipo(request.getParameter("text"), request.getParameter("user_name"));
                response.getWriter().write(responseString);
                break;
            }
            default: {
                response.setStatus(404);
                break;
            }
        }
    }

    private String commandBientotLeWeekend() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("text", "_Est-ce que c'est bientôt le week-end ?_\n" +
                    Jsoup.connect("https://estcequecestbientotleweekend.fr/").get().select(".msg")
                            .stream().map(Element::text).collect(Collectors.joining("\n")));
            jsonObject.put("response_type", "in_channel");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /bientotleweekend: " + e.toString());
            jsonObject.put("text", "Désolé, la slash command n'a pas fonctionné. :ckc:");
        }

        return jsonObject.toString();
    }

    private String commandLangueDeBois() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("text", Jsoup.connect("https://www.faux-texte.com/langue-bois-2.htm").get()
                    .select("#TheTexte p")
                    .stream().map(element -> {
                        String s = element.text();
                        if (!s.endsWith(".")) s += ".";
                        return s;
                    }).collect(Collectors.joining("\n")));
            jsonObject.put("response_type", "in_channel");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /languedebois: " + e.toString());
            jsonObject.put("text", "Désolé, l'appel à faux-texte.com a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private String commandJCVD() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("text", Jsoup.connect("https://www.faux-texte.com/jean-claude-2.htm").get()
                    .select("#TheTexte p").get(0).text());
            jsonObject.put("response_type", "in_channel");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /jcvd: " + e.toString());
            jsonObject.put("text", "Désolé, l'appel à faux-texte.com a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private String commandChuckNorris(boolean onlyGood) {
        JSONObject jsonObject = new JSONObject();

        try {
            if (onlyGood) {
                while (!jsonObject.has("text")) {
                    // only more than 7/10 mark!
                    Document facts = Jsoup.connect("https://chucknorrisfacts.fr/facts/random").get();

                    List<String> text = facts.select(".card-text").stream().map(Element::text).collect(Collectors.toList());
                    List<Float> marks = facts.select(".card-footer span").stream()
                            .map(Element::text)
                            .map(mark -> {
                                // the format is (XXX/10)
                                mark = mark.substring(1, mark.length() - 4);
                                return Float.parseFloat(mark);
                            })
                            .collect(Collectors.toList());

                    for (int i = 0; i < marks.size(); i++) {
                        if (marks.get(i) >= 7) {
                            logger.info("Fact selected: \"" + text.get(i) + "\" with mark " + marks.get(i));

                            jsonObject.put("text", text.get(i));
                            break;
                        }
                    }
                }
            } else {
                // any random fact will do.
                jsonObject.put("text", Jsoup.connect("https://chucknorrisfacts.fr/facts/random").get()
                        .select(".card-text")
                        .get(0).text());
            }

            jsonObject.put("response_type", "in_channel");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /chucknorris: " + e.toString());
            jsonObject.put("text", "Désolé, l'appel à l'API de chucknorrisfacts.fr a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private String commandVacances(String target, String userId) {
        purgeVacances();

        JSONObject jsonObject = new JSONObject();
        if (target != null && !target.isEmpty()) {
            // define a new holiday date.
            try {
                ZonedDateTime targetDateTime = ZonedDateTime.of(LocalDate.parse(target, DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        LocalTime.of(18, 0, 0, 0), ZoneId.of("Europe/Paris"));

                logger.log(Level.INFO, "Countdown target is: " + targetDateTime.toString());

                if (targetDateTime.isBefore(ZonedDateTime.now())) {
                    // date is in the past
                    jsonObject.put("text", "La date que tu m'as donnée (" +
                            targetDateTime.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
                            + ") est dans le passé. Essaie encore. :confused:");
                } else {
                    // date is valid, save it
                    saveTheDate(userId, targetDateTime.toInstant().toEpochMilli());

                    jsonObject.put("text", "OK ! La date de tes vacances (" +
                            targetDateTime.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
                            + ") a bien été enregistrée.\nTape `/vacances` pour avoir un compte à rebours !");
                }
            } catch (DateTimeParseException ex) {
                // date is invalid
                logger.log(Level.WARNING, "Date parse error: " + ex.toString());
                jsonObject.put("text", "Je n'ai pas compris la date que tu m'as donnée en paramètre.");
            }
        } else {
            // find out how much time is left until holidays
            Long targetSaved = findTheDate(userId);
            String message;
            if (targetSaved != null && (message = findDiff(targetSaved)) != null) {
                jsonObject.put("response_type", "in_channel");
                jsonObject.put("text", message);
            } else {
                // user has no holiday saved
                jsonObject.put("text", "Tu n'as pas défini la date de tes vacances.\n" +
                        "Lance la commande `/vacances [JJ/MM/AAAA]` pour le faire.");
            }
        }

        return jsonObject.toString();
    }

    private void purgeVacances() {
        // purge past holidays from database
        final Query query = new Query("vacances").addFilter("target", Query.FilterOperator.LESS_THAN, System.currentTimeMillis() - 86_400_000L);
        for (Entity entity : datastore.prepare(query).asIterable()) {
            logger.info("Deleting holiday " + entity.getKey().toString() + " from the database because date is " +
                    new Date((long) entity.getProperty("target")));
            datastore.delete(entity.getKey());
        }
    }

    private String commandRandom(String numberParameter) {
        JSONObject jsonObject = new JSONObject();

        if (numberParameter == null || numberParameter.isEmpty()) {
            // no number passed
            jsonObject.put("text", "Utilisation : `/random [nombre]`");
        } else {
            try {
                int nb = Integer.parseInt(numberParameter);
                if (nb <= 0) {
                    // negative number passed
                    jsonObject.put("text", "Avec un nombre strictement positif ça marchera mieux !");
                } else {
                    // all good!
                    jsonObject.put("response_type", "in_channel");
                    jsonObject.put("text", "[Tirage entre 1 et " + nb + "] J'ai tiré : " + ((int) (Math.random() * nb + 1)));
                }
            } catch (NumberFormatException e) {
                // what was passed is not a number
                jsonObject.put("text", "Avec un nombre, ça marchera mieux !");
            }
        }
        return jsonObject.toString();
    }

    private void saveTheDate(String userId, long target) {
        Entity datastoreEntity = new Entity("vacances", userId);
        datastoreEntity.setProperty("target", target);

        datastore.put(datastoreEntity);
    }

    private Long findTheDate(String userId) {
        try {
            Entity datastoreEntity = datastore.get(KeyFactory.createKey("vacances", userId));
            return (long) datastoreEntity.getProperty("target");
        } catch (EntityNotFoundException e) {
            return null;
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

    private String findDiff(long time) {
        long now = System.currentTimeMillis();

        long seconds = ((time - now) / (1000));
        long minutes = seconds / 60;
        seconds %= 60;

        long hours = minutes / 60;
        minutes %= 60;

        long days = hours / 24;
        hours %= 24;

        String phrase;

        if (time - now < 0 && time - now > -86_400_000L) {
            phrase = "**C'est les vacances ! \\o/**";
        } else if (time - now < 0) {
            return null;
        } else {
            phrase = "Les vacances sont dans "
                    + (days == 0 ? "" : days + " jour(s), ") + hours + " heure(s), " + minutes + " minute(s) et " + seconds + " seconde(s) !";
        }

        return phrase;
    }

    private String commandTopLyrics(boolean patoche) {
        JSONObject jsonObject = new JSONObject();

        try {
            // fetch song list
            Pair<List<String>, List<String>> results;
            if (patoche) {
                results = fetchPatocheList();
            } else {
                results = fetchSongList();
            }
            List<String> songTitles = results.getLeft();
            List<String> songUrls = results.getRight();

            // pick one at random
            int random = (int) (Math.random() * songTitles.size());
            String titre = songTitles.get(random);
            String url = songUrls.get(random);

            Document lyricsPage = Jsoup.connect(url).get();

            Elements songText = lyricsPage.select(".song-text div");
            String lyricsList = songText.stream()
                    .map(div -> div.childNodes().stream()
                            .map(element -> {
                                if (element instanceof TextNode) {
                                    return ((TextNode) element).text().trim();
                                } else if (element instanceof Element && ((Element) element).tagName().equals("br")) {
                                    return "\n";
                                }
                                return "";
                            })
                            .collect(Collectors.joining()))
                    .collect(Collectors.joining());

            // split the song in blocks like they are on the site
            while (lyricsList.contains("\n\n\n")) lyricsList = lyricsList.replace("\n\n\n", "\n\n");
            String[] lyricsSplit = lyricsList.split("\n\n");

            List<String> parolesFinal = new ArrayList<>();
            for (String parole : lyricsSplit) {
                List<String> lyricsParts = new ArrayList<>();
                lyricsParts.add(parole);

                // piece out the song in 5-line parts maximum, by cutting blocks in half until we're good
                while (lyricsParts.stream().anyMatch(parolePart -> parolePart.split("\n").length > 5)) {
                    List<String> newLyricsParts = new ArrayList<>();
                    for (String lyricsPart : lyricsParts) {
                        if (lyricsPart.split("\n").length > 5) {
                            // split in half
                            String[] lines = lyricsPart.split("\n");
                            StringBuilder firstBlock = new StringBuilder();
                            StringBuilder secondBlock = new StringBuilder();

                            int count = 0;
                            for (String line : lines) {
                                if (count++ < lines.length / 2) firstBlock.append(line).append("\n");
                                else secondBlock.append(line).append("\n");
                            }

                            newLyricsParts.add(firstBlock.toString().trim());
                            newLyricsParts.add(secondBlock.toString().trim());
                        } else {
                            newLyricsParts.add(lyricsPart);
                        }
                    }
                    lyricsParts = newLyricsParts;
                }

                parolesFinal.addAll(lyricsParts);
            }

            // then we just take a block at random and send it out. :p
            String randomLyrics = parolesFinal.get((int) (Math.random() * parolesFinal.size()));
            randomLyrics = "> " + randomLyrics.replace("\n", "\n> ");
            randomLyrics += "\n\n~ " + titre;

            jsonObject.put("response_type", "in_channel");
            jsonObject.put("text", randomLyrics);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /toplyrics or /patoche: " + e.toString());
            jsonObject.put("text", ":ckc:");
        }

        return jsonObject.toString();
    }

    private Pair<List<String>, List<String>> fetchSongList() throws IOException {
        try {
            List<String> songTitles = new ArrayList<>();
            List<String> songUrls = new ArrayList<>();

            String[] artists = new String[]{"Jul", "Aya Nakamura", "Heuss L'Enfoiré", "Gambi"};
            String[] urlLists = new String[]{
                    "https://www.paroles.net/jul", "https://www.paroles.net/aya-nakamura",
                    "https://www.paroles.net/heuss-l-enfoire", "https://www.paroles.net/gambi"};

            for (int i = 0; i < artists.length; i++) {
                final String artiste = artists[i];

                Document page = Jsoup.connect(urlLists[i]).get();

                List<String> titles = new ArrayList<>();
                List<String> urls = new ArrayList<>();

                page.select(".song-listing-extra").get(1).select("a")
                        .forEach(element -> {
                            String songurl = element.attr("href");
                            if (songurl.startsWith("/")) {
                                songurl = "https://www.paroles.net" + songurl;
                            }
                            urls.add(songurl);
                            titles.add(artiste + ", _" + element.text() + "_");
                        });

                logger.log(Level.INFO, artists[i] + " has " + urls.size() + " songs");

                songTitles.addAll(titles);
                songUrls.addAll(urls);
            }

            logger.log(Level.INFO, "Total: " + songUrls.size() + " songs");
            return Pair.of(songTitles, songUrls);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Getting songs broke: " + e.toString());
            throw e;
        }
    }

    private Pair<List<String>, List<String>> fetchPatocheList() throws IOException {
        try {
            List<String> patocheTitles = new ArrayList<>();
            List<String> patocheUrls = new ArrayList<>();

            // hardcoded, we probably won't reach page 3 at any point :p
            for (String url : new String[]{"https://www.paroles.net/patrick-sebastien", "https://www.paroles.net/patrick-sebastien-2"}) {
                Document page = Jsoup.connect(url).get();

                List<String> titres = new ArrayList<>();
                List<String> urls = new ArrayList<>();

                page.select("div[typeof=\"v:Song\"] > .center-on-mobile.box-content")
                        .select("a")
                        .forEach(element -> {
                            String songurl = element.attr("href");
                            if (songurl.startsWith("/")) {
                                songurl = "https://www.paroles.net" + songurl;
                            }
                            urls.add(songurl);
                            titres.add("Patrick Sébastien, _" + element.text() + "_");
                        });

                logger.log(Level.INFO, urls.size() + " chansons added to /patoche");

                patocheTitles.addAll(titres);
                patocheUrls.addAll(urls);
            }

            logger.log(Level.INFO, "Total /patoche: " + patocheUrls.size() + " songs");
            return Pair.of(patocheTitles, patocheUrls);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Getting songs broke: " + e.toString());
            throw e;
        }
    }

    private String commandJoiesDuCode() {
        JSONObject jsonObject = null;
        try {
            while (jsonObject == null) {
                // just try until we succeed :a:
                jsonObject = tryLoadingJoiesDuCode();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /joiesducode: " + e.toString());
            jsonObject.put("text", "Désolé, la récupération a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private JSONObject tryLoadingJoiesDuCode() throws IOException {
        JSONObject jsonObject = new JSONObject();

        Connection.Response resp = Jsoup.connect("https://lesjoiesducode.fr/random").execute();
        String url = resp.url().toString();
        Document content = Jsoup.parse(resp.body());
        String title = content.select(".blog-post-title").text();

        Elements gif = content.select(".blog-post-content object");
        Elements image = content.select(".blog-post-content img");

        String imageUrl = null;
        if (!gif.isEmpty()) {
            imageUrl = gif.attr("data");
        } else if (!image.isEmpty()) {
            imageUrl = image.attr("src");
        }

        if (imageUrl == null || (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://"))) {
            // if the result is anything other than an URL, just return null and we'll retry.
            return null;
        } else {
            HashMap<String, String> attachment = new HashMap<>();
            attachment.put("fallback", title + " : " + url);
            attachment.put("title", title);
            attachment.put("image_url", imageUrl);

            jsonObject.put("attachments", Collections.singletonList(attachment));
        }

        jsonObject.put("response_type", "in_channel");
        return jsonObject;
    }

    private static class CoronavirusStats {
        String country;
        int confirmed;
        int deaths;
        int recovered;

        public CoronavirusStats(String country, int confirmed, int deaths, int recovered) {
            this.country = country;
            this.confirmed = confirmed;
            this.deaths = deaths;
            this.recovered = recovered;
        }
    }

    private String commandCoronavirus() {
        JSONObject jsonObject = new JSONObject();

        try {
            // we need to spoof the user agent because a lot of things (CloudFlare?) hate Java 8 for some reason
            HttpURLConnection connection = (HttpURLConnection) new URL("https://disease.sh/v2/countries?sort=cases").openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0");
            connection.connect();

            // get coronavirus stats and aggregate them
            JSONArray countries = new JSONArray(IOUtils.toString(connection.getInputStream(), UTF_8));
            ArrayList<CoronavirusStats> stats = new ArrayList<>(countries.length());
            for (Object country : countries) {
                JSONObject countryObj = (JSONObject) country;
                stats.add(new CoronavirusStats(countryObj.getString("country"),
                        countryObj.getInt("cases"),
                        countryObj.getInt("deaths"),
                        countryObj.getInt("recovered")));
            }

            DecimalFormat format = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.FRANCE));

            // aggregated stats
            StringBuilder message = new StringBuilder("**Statistiques du coronavirus**\n__Dans le monde :__ ");
            message.append(format.format(stats.stream().mapToInt(s -> s.confirmed).sum())).append(" cas, ");
            message.append(format.format(stats.stream().mapToInt(s -> s.deaths).sum())).append(" morts, ");
            message.append(format.format(stats.stream().mapToInt(s -> s.recovered).sum())).append(" guéris\n__En France (**");

            // stats for France
            CoronavirusStats frenchStats = stats.stream().filter(s -> s.country.equals("France")).findFirst()
                    .orElseThrow(() -> new Exception("aaaa la France n'existe pas"));
            int francePosition = stats.indexOf(frenchStats) + 1;
            message.append(francePosition).append(francePosition == 1 ? "er" : "e").append("**) :__ ");
            message.append(format.format(frenchStats.confirmed)).append(" cas, ");
            message.append(format.format(frenchStats.deaths)).append(" morts, ");
            message.append(format.format(frenchStats.recovered)).append(" guéris\n\n__Top 5 :__\n");

            // top 5 countries
            int position = 1;
            for (CoronavirusStats countryStats : stats) {
                message.append("**").append(countryStats.country).append("** : ");
                message.append(format.format(countryStats.confirmed)).append(" cas, ");
                message.append(format.format(countryStats.deaths)).append(" morts, ");
                message.append(format.format(countryStats.recovered)).append(" guéris\n");

                if (++position > 5) break;
            }

            jsonObject.put("text", message.toString().trim());
            jsonObject.put("response_type", "in_channel");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /coronavirus: " + e.toString());
            jsonObject.put("text", "Désolé, la récupération de la base de données a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private String commandFakeName() {
        JSONObject jsonObject = new JSONObject();

        try {
            Document document = Jsoup.connect("https://www.fakenamegenerator.com/gen-random-fr-fr.php").get();
            String name = "**" + document.select(".address h3").text() + "**\n"
                    + document.select(".address .adr").html().replace("<br>", "").trim();

            jsonObject.put("response_type", "in_channel");
            jsonObject.put("text", name);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /fakename: " + e.toString());
            jsonObject.put("text", "Désolé, la récupération a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private static class VideoTendance {
        public int views;
        public ZonedDateTime publishedDate;
        public String author;
        public String title;
        public int position;
        public String id;
    }

    private String commandTendancesYoutube() {
        JSONObject jsonObject = new JSONObject();

        try {
            List<VideoTendance> tendancesYoutube = refreshTendancesYoutube();
            VideoTendance video = tendancesYoutube.get((int) (Math.random() * tendancesYoutube.size()));

            String text = "**" + video.title + "**\n" +
                    "_Par " + video.author + ", publié le " + video.publishedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH'h'mm", Locale.FRENCH)) + ", "
                    + new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.FRENCH)).format(video.views) + " vues" +
                    " - #" + video.position + " des tendances_\n:arrow_right: "
                    + "https://youtu.be/" + video.id;

            jsonObject.put("response_type", "in_channel");
            jsonObject.put("text", text);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem while getting YouTube trends: " + e.toString());
            jsonObject.put("text", "Désolé, la récupération a échoué. :ckc:");
        }

        return jsonObject.toString();
    }

    private List<VideoTendance> refreshTendancesYoutube() throws IOException {
        logger.log(Level.INFO, "Refresh YouTube trends");

        List<VideoTendance> tendancesYoutube = new ArrayList<>();

        JSONObject youtubeResponse = new JSONObject(IOUtils.toString(new URL("https://www.googleapis.com/youtube/v3/videos?hl=fr&maxResults=50" +
                "&regionCode=FR&chart=mostPopular&part=snippet,statistics&key=" + SecretConstants.YOUTUBE_API_KEY).openStream(), UTF_8));

        for (Object video : youtubeResponse.getJSONArray("items")) {
            try {
                VideoTendance videoObj = new VideoTendance();
                JSONObject snippet = ((JSONObject) video).getJSONObject("snippet");
                JSONObject statistics = ((JSONObject) video).getJSONObject("statistics");
                JSONObject localized = snippet.getJSONObject("localized");

                videoObj.author = snippet.getString("channelTitle");
                videoObj.publishedDate = ZonedDateTime.parse(snippet.getString("publishedAt"));
                videoObj.title = localized.getString("title");
                videoObj.views = Integer.parseInt(statistics.getString("viewCount"));
                videoObj.position = tendancesYoutube.size() + 1;
                videoObj.id = ((JSONObject) video).getString("id");
                tendancesYoutube.add(videoObj);
            } catch (Exception e) {
                logger.warning("Could not parse video: " + e);
            }
        }

        if (tendancesYoutube.isEmpty()) {
            throw new IOException("There is no valid video! :a:");
        }

        logger.log(Level.INFO, tendancesYoutube.size() + " videos found");
        return tendancesYoutube;
    }

    private String commandPutaclic() {
        JSONObject jsonObject = new JSONObject();

        try {
            Document document = Jsoup.connect("http://www.le-toaster.fr/generateur-buzz/").get();
            String text = document.select("article h2").text().trim();

            jsonObject.put("response_type", "in_channel");
            jsonObject.put("text", text);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /putaclic: " + e.toString());
            jsonObject.put("text", "Désolé, la récupération a échoué. :ckc:");
        }

        return jsonObject.toString();
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

    private String commandRandomParrot() {
        try {
            Map<String, String> parrots = ParrotQuickImporterService.getParrots();
            List<String> parrotNames = new ArrayList<>(parrots.keySet());

            final int selected = (int) (Math.random() * parrotNames.size());

            HashMap<String, String> attachment = new HashMap<>();
            attachment.put("fallback", parrotNames.get(selected) + " : " + parrots.get(parrotNames.get(selected)));
            attachment.put("title", parrotNames.get(selected));
            attachment.put("image_url", parrots.get(parrotNames.get(selected)));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("response_type", "in_channel");
            jsonObject.put("attachments", Collections.singletonList(attachment));
            return jsonObject.toString();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /randomparrot: " + e.toString());

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la récupération des parrots a échoué. :ckc:");
            return jsonObject.toString();
        }
    }

    private String commandMonkeyUser() {
        List<String> links = new ArrayList<>();
        List<String> names = new ArrayList<>();

        final String sourceCode;

        try {
            sourceCode = IOUtils.toString(new URL("https://www.monkeyuser.com/").openStream(), UTF_8);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem with /monkeyuser: " + e);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la consultation de Monkey User a échoué. :ckc:");
            return jsonObject.toString();
        }

        // comics are all in the source code of the page... in JS form. Extract them using epic regex
        final Pattern imageRegex = Pattern.compile("^\\s+images\\.push\\(\"([^\"]+)\"\\);");
        final Pattern titlesRegex = Pattern.compile("^\\s+titles\\.push\\(\"([^\"]+)\"\\);");
        for (String line : sourceCode.split("\n")) {
            Matcher m = imageRegex.matcher(line);
            if (m.matches()) {
                links.add("https://www.monkeyuser.com/assets/images/" + m.group(1));
            }

            m = titlesRegex.matcher(line);
            if (m.matches()) {
                names.add(m.group(1));
            }
        }

        if (links.size() == 0 || links.size() != names.size()) {
            // there are no links or we have more/less comic links than titles. it means there probably is a problem somewhere...
            logger.log(Level.SEVERE, "Problem while getting Monkey User comics: " + links.size() + " links and " + names.size() + " names found");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la consultation de Monkey User a échoué. :ckc:");
            return jsonObject.toString();
        }

        int selected;
        do {
            selected = (int) (Math.random() * links.size());
        } while (!links.get(selected).toLowerCase(Locale.ROOT).endsWith(".png"));

        HashMap<String, String> attachment = new HashMap<>();
        attachment.put("fallback", names.get(selected) + " : " + links.get(selected));
        attachment.put("title", names.get(selected));
        attachment.put("image_url", links.get(selected));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response_type", "in_channel");
        jsonObject.put("attachments", Collections.singletonList(attachment));
        return jsonObject.toString();
    }

    private String commandXkcd() {
        final Document xkcd;

        try {
            xkcd = Jsoup.connect("https://c.xkcd.com/random/comic/").get();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Problem while getting xkcd comic: " + e);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", "Désolé, la consultation de xkcd a échoué. :ckc:");
            return jsonObject.toString();
        }

        String title = xkcd.select("#ctitle").text();
        String image = "https:" + xkcd.select("#comic img").attr("src");
        String description = xkcd.select("#comic img").attr("title");

        HashMap<String, String> attachment = new HashMap<>();
        attachment.put("fallback", title + " : " + image + "\n" + description);
        attachment.put("title", title);
        attachment.put("image_url", image);
        attachment.put("text", description);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("response_type", "in_channel");
        jsonObject.put("attachments", Collections.singletonList(attachment));
        return jsonObject.toString();
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

    private String commandInfoPipo() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("response_type", "in_channel");
            jsonObject.put("text", "`" + IOUtils.toString(new URL("https://www.luc-damas.fr/pipotron/fail_geek/"), UTF_8) + "`");
            return jsonObject.toString();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Problem while getting info pipo: " + e);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("text", ":ckc:");
            return jsonObject.toString();
        }
    }

    /**
     * Ported from a website I cannot find anymore.
     */
    private String commandEddy() {
        final String[][] generator = {
                {"Chapitre abstrait 3 du conpendium :", "C’est à dire ici, c’est le contraire, au lieu de panacée,", "Au nom de toute la communauté des savants,", "Lorsqu’on parle de tous ces points de vues,", "C’est à dire quand on parle de ces rollers,", "Quand on parle de relaxation,", "Nous n’allons pas seulement danser ou jouer au football,", "D'une manière ou d'une autre,", "Quand on prend les triangles rectangles,", "Se consolidant dans le système de insiding et outsiding,", "Lorsque l'on parle des végétaliens, du végétalisme,", "Contre la morosité du peuple,", "Tandis que la politique est encadrée par des scientifiques issus de Sciences Po et Administratives,", "On ne peut pas parler de politique administrative scientifique,", "Pour emphysiquer l'animalculisme,", "Comme la coumbacérie ou le script de Aze,", "Vous avez le système de check-up vers les anti-valeurs, vous avez le curuna, or", "La convergence n’est pas la divergence,", "L’émergence ici c’est l’émulsion, c’est pas l’immersion donc", "Imbiber, porter", "Une semaine passée sans parler du peuple c’est errer sans abri, autrement dit", "Actuellement,", "Parallèlement,", "Mesdames et messieurs fidèles,"},
                {"la cosmogonisation", "l'activisme", "le système", "le rédynamisme", "l'ensemble des 5 sens", "la société civile", "la politique", "la compétence", "le colloque", "la contextualisation", "la congolexicomatisation", "la congolexicomatisation", "la congolexicomatisation", "la congolexicomatisation", "la prédestination", "la force", "la systématique", "l'ittérativisme", "le savoir", "l'imbroglio", "la concertation politique", "la délégation", "la pédagogie", "la réflexologie"},
                {"vers la compromettance pour des saint-bioules", "vers ce qu’on appelle la dynamique des sports", "de la technicité informatisée", "de la Théorie Générale des Organisations", "autour de la Géo Physique Spatiale", "purement technique", "des lois du marché", "de l'orthodoxisation", "inter-continentaliste", "à l'égard de la complexité", "éventualiste sous cet angle là", "de toute la République Démocratique du Congo", "à l'incognito", "autour de l'ergonométrie", "indispensable(s) en science et culture", "autour de phylogomènes généralisés", "à forciori,", "par rapport aux diplomaties"},
                {"tend à ", "nous pousse à ", "fait allusion à ", "va ", "doit ", "consiste à ", "nous incite à", "vise à", "semble", "est censé(e)", "paraît", "peut", "s'applique à", "consent à", "continue à", "invite à", "oblige à", "parvient à", "pousse à", "se résume à", "suffit à", "se résoud à", "sert à", "tarde à"},
                {"incristaliser", "imposer", "intentionner ", "mettre un accent sur ", "tourner ", "informatiser ", "aider ", "défendre ", "gérer ", "prévaloir ", "vanter ", "rabibocher", "booster", "porter d'avis sur ce qu'on appelle", "cadrer", "se baser sur", "effaceter", "réglementer", "régler", "faceter", "partager", "uniformiser", "défendre", "soutenir", "propulser", "catapulter", "établir"},
                {"les interchanges", "mes frères propres", "les revenus", "cette climatologie", "une discipline", "la nucléarité", "l'upensmie", "les sens dynamitiels", "la renaissance africaine", "l'estime du savoir", "une kermesse", "une certaine compétitivité", "cet environnement de 2 345 410 km²", "le kilométrage", "le conpemdium", "la quatripartie", "les encadrés", "le point adjacent", "la bijectivité", "le panafricanisme", "ce système phénoménal", "le système de Guipoti : 1/B+1/B’=1/D", "une position axisienne", "les grabuses lastiques", "le chicouangue", "le trabajo, le travail, la machinale, la robotisation", "les quatre carrés fous du fromage"},
                {"autour des dialogues intercommunautaires", "provenant d'une dynamique syncronique", "vers le monde entier", "propre(s) aux congolais", "vers Lovanium", "vers l'humanisme", "comparé(e)(s) la rénaque", "autour des gens qui connaissent beaucoup de choses", "possédant la francophonie", "dans ces prestances", "off-shore", "dans Kinshasa", "dans la sous-régionalité", "dans le prémice", "belvédère", "avec la formule 1+(2x5)", "axé(e)(s) sur la réalité du terrain", "dans les camps militaires non-voyants", "avéré(e)(s)", "comme pour le lancement de Troposphère V"},
                {", tu sais ça", ", c’est clair", ", je vous en prie", ", merci", ", mais oui", ", Bonne Année", ", bonnes fêtes"}
        };

        String s = "";
        for (String[] parts : generator) {
            String chosen = parts[(int) (Math.random() * parts.length)];

            s += " " + chosen;
        }

        s = s.substring(1).replace("  ", " ").replace(" ,", ",") + ".";

        JSONObject outputJSON = new JSONObject();

        outputJSON.put("response_type", "in_channel");
        outputJSON.put("text", s);
        return outputJSON.toString();
    }

    /**
     * Ported from https://www.pipotronic.com/
     */
    private String commandPipo(String param, String userName) {
        int count = 1;
        String phrases = "";

        String supMessage = "";

        if (param != null && !param.trim().isEmpty()) {
            try {
                count = Integer.parseInt(param);

                if (count > 5) {
                    // force-cap to 5
                    supMessage = "@" + userName + ": J'envoie 5 pipos au maximum. Ca suffit...";
                    count = 5;
                } else if (count <= 0) {
                    // messages in case the user invokes with 0 or less
                    switch ((int) (Math.random() * 5)) {
                        case 0:
                            phrases = "Tu veux que je te réponde quoi au juste là ?";
                            break;
                        case 1:
                            phrases = "T'es sérieux ?";
                            break;
                        case 2:
                            phrases = "...";
                            break;
                        case 3:
                            phrases = "<rien du tout>";
                            break;
                        case 4:
                            phrases = "Hum... " + count + " pipos ? T'essaierais pas de me pipoter là ?";
                            break;
                    }

                    JSONObject outputJSON = new JSONObject();

                    outputJSON.put("response_type", "in_channel");
                    outputJSON.put("text", phrases);
                    return outputJSON.toString();
                }
            } catch (NumberFormatException nfe) {
                // invalid number, will default to 1
                supMessage = "@" + userName + ": La prochaine fois, passe-moi un nombre en paramètre ;)";
            }
        }

        for (int l = 0; l < count; l++) {
            String[][] pipo = {
                    {"Face à", "Relativement à", "Pour optimiser", "Pour accentuer", "Afin de maîtriser", "Au moyen d#", "Depuis l'émergence d#", "Pour challenger", "Pour défier",
                            "Pour résoudre", "En termes de redynamisation d#", "Concernant l'implémentation d#", "À travers", "En s'orientant vers", "En termes de process, concernant",
                            "En rebondissant sur", "Pour intégrer", "Une fois internalisée", "Pour externaliser", "Dans la lignée d#", "En synergie avec",
                            "Là où les benchmarks désignent", "Au cœur d#", "En auditant", "Une fois evaluée", "Partout où domine", "Pour réagir à", "En jouant", "Parallèlement à",
                            "Malgré", "En réponse à", "En réaction à", "Répliquant à", "En phase de montée en charge d#", "En réponse à", "En phase de montée en charge d#", "Grâce à",
                            "Perpendiculairement à", "Indépendamment d#", "Corrélativement à", "Tangentiellement à", "Concomitamment à", "Par l'implémentation d#"
                    },
                    {"la problématique", "l'opportunité", "la mondialisation", "une globalisation", "la bulle", "la culture", "la synergie", "l'efficience", "la compétitivité",
                            "une dynamique", "une flexibilité", "la revalorisation", "la crise", "la stagflation", "la convergence", "une réactivité", "une forte croissance",
                            "la gouvernance", "la prestation", "l'offre", "l'expertise", "une forte suppléance", "une proposition de valeur", "une supply chain", "la démarche",
                            "une plate-forme", "une approche", "la mutation", "l'adaptabilité", "la pluralité", "une solution", "la multiplicité", "la transversalité",
                            "la mutualisation"
                    },
                    {"opérationnelle,", "quantitative,", "des expertises,", "porteuse,", "autoporteuse,", "collaborative,", "accélérationnelle,", "durable,", "conjoncturelle,",
                            "institutionnelle,", "managériale,", "multi-directionnelle,", "communicationnelle,", "organisationnelle,", "entrepreneuriale,", "motivationnelle,",
                            "soutenable,", "qualitative,", "stratégique,", "interne / externe,", "online / offline,", "situationnelle,", "référentielle,", "institutionnelle,",
                            "globalisante,", "solutionnelle,", "opérationnelle,", "compétitionnelle,", "gagnant-gagnant,", "interventionnelle,", "sectorielle,", "transversale,",
                            "des prestations,", "ambitionnelle,", "des sous-traitances,", "corporate,", "asymétrique,", "budget", "référentielle"
                    },
                    {"les cadres doivent ", "les personnels concernés doivent ", "les personnels concernés doivent ", "les N+1 doivent ", "le challenge consiste à",
                            "le défi est d#", "il faut", "on doit", "il faut", "on doit", "il faut", "on doit", "il faut", "on doit", "chacun doit", "les fournisseurs vont",
                            "les managers décident d#", "les acteurs du secteur vont", "les responsables peuvent", "la conjecture peut", "il est impératif d#",
                            "un meilleur relationnel permet d#", "une ambition s'impose :", "mieux vaut", "le marché exige d#", "le marché impose d#", "il s'agit d#",
                            "voici notre ambition :", "une réaction s'impose :", "voici notre conviction :", "les bonnes pratiques consistent à", "chaque entité peut",
                            "les décideurs doivent", "il est requis d#", "les sociétés s'engagent à", "les décisionnaires veulent", "les experts doivent",
                            "la conjecture pousse les analystes à", "les structures vont", "il faut un signal fort :", "la réponse est simple :", "il faut créer des occasions :",
                            "la réponse est simple :", "l'objectif est d#", "l'objectif est évident :", "l'ambition est claire :", "chaque entité doit", "une seule solution :",
                            "il y a nécessité d#", "il est porteur d#", "il faut rapidement", "il faut muscler son jeu : ", "la réponse client permet d#",
                            "la connaissance des paramètres permet d#", "les éléments moteurs vont"
                    },
                    {"optimiser", "faire interagir", "capitaliser sur", "prendre en considération", "anticiper ", "intervenir dans", "imaginer", "solutionner", "piloter",
                            "dématerialiser", "délocaliser", "coacher", "investir sur", "valoriser", "flexibiliser", "externaliser", "auditer", "sous-traiter", "revaloriser", "habiliter",
                            "requalifier", "revitaliser", "solutionner", "démarcher", "budgetiser", "performer", "incentiver", "monitorer", "segmenter", "désenclaver", "décloisonner",
                            "déployer", "réinventer", "flexibiliser", "optimiser", "piloter", "révolutionner", "gagner", "réussir", "connecter", "faire converger", "planifier",
                            "innover sur", "monétiser", "concrétiser", "impacter", "transformer", "prioriser", "chiffrer", "initiativer", "budgetiser", "rénover", "dominer"
                    },
                    {"solutions", "issues", "axes mobilisateurs", "problématiques", "cultures", "alternatives", "interactions", "issues", "expertises", "focus", "démarches",
                            "alternatives", "thématiques", "atouts", "ressources", "applications", "applicatifs", "architectures", "prestations", "process", "performances", "bénéfices",
                            "facteurs", "paramètres", "capitaux", "sourcing", "émergences", "kick-off", "recapitalisations", "produits", "frameworks", "focus", "challenges", "décisionnels",
                            "ouvertures", "fonctionnels", "opportunités", "potentiels", "territoires", "leaderships", "applicatifs", "prestations", "plans sociaux", "wordings",
                            "harcèlements", "monitorings", "montées en puissance", "montées en régime", "facteurs", "harcèlements", "référents", "éléments", "nécessités",
                            "partenariats", "retours d'expérience", "dispositifs", "potentiels", "intervenants", "directives", "directives", "perspectives", "contenus", "implications",
                            "kilo-instructions", "supports", "potentiels", "mind mappings", "thématiques", "workshops", "cœurs de mission", "managements", "orientations", "cibles"
                    },
                    {"métier", "prospect", "customer", "back-office", "client", "envisageables", "à l'international", "secteur", "client", "vente", "projet", "partenaires", "durables",
                            "à forte valeur ajoutée", "soutenables", "chiffrables", "évaluables", "force de vente", "corporate", "fournisseurs", "bénéfices", "convivialité",
                            "compétitivité", "investissement", "achat", "performance", "à forte valeur ajoutée", "dès l'horizon 2020", "à fort rendement", "qualité", "logistiques",
                            "développement", "risque", "terrain", "mobilité", "praticables", "infrastructures", "organisation", "projet", "recevables", "investissement",
                            "conseil", "conseil", "sources", "imputables", "intermédiaires", "leadership", "pragmatiques", "framework", "coordination", "d'excellence", "stratégie",
                            "de confiance", "crédibilité", "compétitivité", "méthodologie", "mobilité", "efficacité", "efficacité"
                    }
            };

            String[] selectedPipos = new String[pipo.length];
            int i = 0;
            for (String[] pipoLine : pipo) {
                selectedPipos[i++] = pipoLine[(int) (Math.random() * pipoLine.length)];
            }

            for (i = 0; i < selectedPipos.length - 1; i++) {
                if (selectedPipos[i].endsWith("#")) {
                    if (selectedPipos[i + 1].startsWith("a")
                            || selectedPipos[i + 1].startsWith("e")
                            || selectedPipos[i + 1].startsWith("i")
                            || selectedPipos[i + 1].startsWith("o")
                            || selectedPipos[i + 1].startsWith("u")
                            || selectedPipos[i + 1].startsWith("y")) {
                        selectedPipos[i] = selectedPipos[i].substring(0, selectedPipos[i].length() - 1) + "'";
                    } else {
                        selectedPipos[i] = selectedPipos[i].substring(0, selectedPipos[i].length() - 1) + "e ";
                    }
                } else {
                    selectedPipos[i] += " ";
                }
            }

            String phrase = (selectedPipos[0] + " "
                    + selectedPipos[1] + " "
                    + selectedPipos[2] + " "
                    + selectedPipos[3] + " "
                    + selectedPipos[4] + " les "
                    + selectedPipos[5] + " "
                    + selectedPipos[6] + ".");

            while (phrase.contains("  "))
                phrase = phrase.replace("  ", " ");
            phrase = phrase.replace("' ", "'");
            phrase = phrase.replace(" .", ".");

            phrase = "_" + phrase + "_";

            if (phrases.isEmpty()) phrases = phrase;
            else phrases += "\n" + phrase;
        }

        if (!supMessage.isEmpty()) {
            phrases += "\n" + supMessage;
        }

        JSONObject outputJSON = new JSONObject();
        outputJSON.put("response_type", "in_channel");
        outputJSON.put("text", phrases);
        return outputJSON.toString();
    }

    private String commandWeekend(String userName) {
        String endingPhrase = "*C'est le week-end \\o/*";
        String notFinishedPhrase = "@" + userName + ", le week-end est dans ";

        // weekend is at 6pm on Friday. Go back if today is Saturday or Sunday.
        ZonedDateTime zdt = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Paris"));
        while (zdt.getDayOfWeek() != DayOfWeek.FRIDAY) {
            if (Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(zdt.getDayOfWeek())) {
                zdt = zdt.minusDays(1);
            } else {
                zdt = zdt.plusDays(1);
            }
        }
        zdt = zdt.withHour(18).withMinute(0).withSecond(0).withNano(0);

        long time = zdt.toInstant().toEpochMilli();
        long now = System.currentTimeMillis();

        long seconds = ((time - now) / (1000));
        long minutes = seconds / 60;
        seconds %= 60;

        long hours = minutes / 60;
        minutes %= 60;

        long days = hours / 24;
        hours %= 24;

        JSONObject outputJSON = new JSONObject();
        String phrase;
        if (time - now < 0) {
            phrase = endingPhrase;
        } else {
            phrase = notFinishedPhrase
                    + (days == 0 ? "" : days + " jour(s), ") + hours + " heure(s), " + minutes + " minute(s) et " + seconds + " seconde(s) !";
        }

        outputJSON.put("response_type", "in_channel");
        outputJSON.put("text", phrase);
        return outputJSON.toString();
    }

    /**
     * This is ported straight from JS from https://lunatopia.fr/blog/films-de-noel
     */
    private static class ChristmasFilmGenerator {
        private static final String[] titre = new String[]{"12 cadeaux pour Noël", "Une mariée pour Noël", "Le détour de Noël",
                "Un duo pour Noël", "La mélodie de Noël", "Un Noël de conte de fées", "Un Noël de rêve", "Un rêve pour Noël",
                "Des cookies pour Noël", "La mariée de décembre", "Un mariage pour Noël", "Un cadeau de rêve", "Un Noël parfait",
                "Un Noël royal", "Un prince pour Noël", "Un vœux pour Noël", "Le meilleur Noël", "Noël en famille", "L'aventure de Noël",
                "La buche de Noël", "Un noël enchanté", "Le meilleur des cadeaux", "Un cadeau de rêve", "L'amour en cadeau", "Un amour de Noël",
                "Le Noël de l'amour", "L'amour en héritage", "L'héritage de Noël", "Une surprise pour Noël", "Une lettre au père Noël", "La liste de Noël",
                "Un Noël à emporter", "Noël en fête", "Noël sous les étoiles", "Une couronne pour Noël", "Un Noël courronné", "Je t'aime comme Noël",
                "Épouse moi pour Noël", "Miss Noël", "Mister Noël", "Mon amour de Noël", "Mon rêve de Noël", "Un rêve pour Noël", "Des vacances de rêve",
                "Il était une fois à Noël", "Le calendrier de l'avent", "Orgueil, préjugés et cadeaux sous le sapin", "De l'amour sous le sapin",
                "Un baiser sous le gui", "L'amour sous le gui", "Réunion de Noël", "Des retrouvailles à Noël", "En route pour Noël", "Mariée d'hiver",
                "La promesse de Noël", "Une promesse sous le gui", "Le secret de Noël", "Un secret pour Noël", "Un secret sous le sapin",
                "Le plus beau jour de l'année", "Le plus beau Noël de ma vie", "Le plus beau cadeau de ma vie", "Les neufs vies de Noël", "Le plus beau des Noël",
                "Le plus doux des Noël", "La carte de Noël"};

        private static final String[] nomMeuf = new String[]{"Lacey Charm", "Alicia Witt", "Ambert McIver", "Ava Mitchell", "April McDonagall", "Kristin Elliott",
                "Candace Cameron Burke", "Emily McLayne", "Jessie Jones", "Candice Everdeen", "Kaylynn Riley McAlistair", "Alexandra McKintosh", "Abbey Patterson",
                "Cate Sweetin", "Jodie Walker", "Belinda Shaw", "Merritt Patterson", "Nancy Davis", "Candy McLair", "Donna Mills", "Christie Reynolds", "Pennie Miller"};

        private static final String[] nomMec = new String[]{"Nick Cane", "Richard Wright", "Gabriel Hogan", "Edgar Jones", "Rory Gallagher", "Sam Page", "Gabe Walker",
                "Eon Bailey", "Brennan Hebert", "Dylon O'Neil", "Henri Walsh", "Andrew Mann", "Dustin McGary", "Matthew McDonagall", "Brian McAlistair"};

        private static final String[] etat = new String[]{"Ohio", "Nebraska", "Wisconsin", "Wyoming", "Oregon", "Montana", "Minnesota", "Maine", "Vermont",
                "Connecticut", "Kentucky", "Texas", "Missouri", "Illinois", "Indiana", "Arizona", "Arkansas", "Oklahoma", "Iowa", "Kansas", "Colorado",
                "Idaho", "Nevada", "Utah"};

        private static final String[] petiteVille = new String[]{"Ridgetown", "Riverside", "Winslow", "Eureka", "Carmel", "New Castle", "Crystal River", "Franklin",
                "Fairfield", "Greenville", "Kingston", "Springfield", "Arlington", "Georgetown", "Madison", "Salen", "Old Lebanon", "Port Clinton", "Ashland",
                "Ashville", "Fort Jackson", "Milton", "Newport", "Clayton", "Dayton", "Lexington", "Milford", "Winchester", "Port Hudson", "Davenportside", "Burbank",
                "Lakewood", "Marion Falls", "Sioux Falls", "Edison", "Arlingwood", "Ann Arbor", "Mary Valley", "Thousand Oaks", "Treehills", "Kentford",
                "Port New Haven", "Crystal Falls"};

        private static final String[] grandeVille = new String[]{"New-York", "Los Angeles", "Chicago", "San Francisco", "Seattle", "Washington", "Las Vegas", "Manhattan"};

        private static final String[] metierMeuf = new String[]{"styliste", "décoratrice d'intérieur", "photographe de mode", "photographe de sports extrêmes", "avocate",
                "agent immobilier", "illustratrice de livres pour enfants", "pianiste de renommée mondiale", "cheffe d'entreprise", "publiciste", "graphiste",
                "directrice de communication", "guide touristique", "journaliste dans la mode", "architecte", "architecte d'intérieur", "organisatrice de mariage"};

        private static final String[] metierMec = new String[]{"directeur d'école", "éleveur", "fermier", "ébeniste", "potier", "céramiste", "libraire", "vétérinaire",
                "fleuriste", "sculpteur sur glace"};

        private static final String[] histoire = new String[]{"laMeuf, avocate dans l'immobilier, fait la rencontre de leMec, un petit libraire qui essaye d'empêcher la construction du centre commercial dont elle s'occupe. Va-t-elle abandonner ses projets par amour ?",
                "laMeuf, metierCool qui travaille beaucoup trop, est obligée de retourner dans son etatPaume natal pour s'occuper de l'héritage de sa grand-mère et fait la rencontre de leMec, jeune vétérinaire de la ville. Entre sa vie à bigCity avec un salaire à 5 chiffre et les plaisirs simples de la campagne, le choix va être difficile&nbsp;!",
                "laMeuf, metierCool à bigCity, rentre à contre-cœur passer les fêtes en famille. Les choses empirent quand elle se retrouve obligée à faire équipe avec son ex, leMec, pour la chasse au trésor de Noël de villePaume, etatPaume, sa ville natale.",
                "Dans 4 jours, laMeuf doit épouser un ambitieux milliardaire, mais sa rencontre avec leMec, le traiteur de la cérémonie, va tout remettre en question.",
                "Très investie dans sa carrière de metierCool, laMeuf retourne à villePaume, sa ville natale, pour veiller sur sa grand-mère. Celle-ci lui présente leMec, jeune metierVrai, qui aurait bien besoin d'aide pour organiser le bal de Noël.",
                "À quelques semaines de Noël, laMeuf est embauchée pour décorer les locaux de CandyCane Corp. Elle devra composer avec leMec, le PDG, qui a succédé à son père à la tête de l'entreprise, mais déteste Noël.",
                "Quand une jeune mariée se voit accorder un vœux par l'ange de Noël, elle souhaite devenir célibataire à nouveau. Mais sa vie de femme libre n'est pas aussi épanouissante qu'elle l'aurait cru, et elle se met en tête de reconquérir son mari.",
                "laMeuf et Mandie McKinnie sont rivales depuis que cette dernière a triché au concours annuel de cookies en 4e. Elles travaillent désormais dans la même école, et sont en compétition constante. L'arrivée en ville de leMec, un jeune metierVrai amoureux de la patisserie qui propose de faire renaître le concours annuel de cookies va mettre le feu aux poudres&nbsp;!",
                "laMeuf, jeune divorcée et metierCool, donnerait tout pour sa fille Emma. Forcée de fermer sa boutique de bigCity, le retour dans son etatPaume natal est rude. Heureusement, le professeur de musique d'Emma, leMec, les aide à s'ajuster à cette nouvelle vie.",
                "Deux New-Yorkais se retrouvent bloqués par une tempête de Neige à villePaume, etatPaume. laMeuf, désespérément romantique, doit trouver un moyen de se rendre chez ses beaux parents à temps pour sa soirée de fiançailles. Heureusement, leMec, célibataire endurci, va l'aider&hellip;",
                "Alors qu'elle vient d'abandonner son 3ème fiancé à l'autel, laMeuf jure de renoncer à toute relation jusqu'à ce qu'elle trouve «&nbsp;le bon&nbsp;». Mais sa rencontre avec leMec, metierVrai, célibataire endurci plein de charme, va mettre sa promesse à rude épreuve.",
                "Lors d'une soirée un peu trop arrosée, leMec a fait un pari&nbsp;: il doit convaincre une femme de l'épouser avant Noël, soit dans 4 semaines&nbsp;! Il jette son dévolu sur laMeuf, la talentueuse metierCool qu'il vient d'embaucher.",
                "laMeuf vient de perdre son emploi de metierCool. Elle rencontre par hasard leMec, patron surbooké qui la charge d'acheter ses cadeaux de Noël. Elle va lui transmettre l'esprit de Noël et bien plus encore&hellip;",
                "Quand on l'envoie superviser les travaux de rénovation d'un hôtel perdu à villePaume, etatPaume, laMeuf s'attends à passer le pire Noël de sa vie. C'est compter sans la présence de leMec, conducteur de chantier bourru qui va lui faire découvrir le charme de la vie à la campagne&hellip;",
                "laMeuf, journaliste à bigCity, bâcle un article capital. En guise de punition, on l'envoie à villePaume, etatPaume, faire un reportage sur leMec, metierVrai. Il est veuf, revêche et taciturne. Ils se détestent. Puis la magie de Noël entre en jeu&hellip;",
                "Fraîchement plaquée par son mari, laMeuf retourne vivre chez ses parents dans son etatPaume natal. Elle fait la rencontre de leMec, metierVrai grincheux et moqueur. Ils se détestent au premier regard&hellip;",
                "Lorsque laMeuf, metierCool, rencontre leMec, elle est loin de se douter qu'il est le prince héritier de Cénovie en visite incognito à villePaume, etatPaume.",
                "laMeuf, jeune héritière frivole, est envoyée par ses parents à villePaume, etatPaume pour apprendre la valeur du travail et le sens des autres. Heureusement, leMec, metierVrai, saura l'aider&hellip;",
                "La ville de villePaume, etatPaume est frappée par une tempête de neige. laMeuf, metierCool, est bloquée plusieurs jours et doit apprendre la patience. Heureusement, leMec, jeune metierVrai bourru, est là pour l'aider.",
                "Tout juste débarquée à villePaume, etatPaume après une rupture difficile, laMeuf, jeune citadine metierCool, fait la connaissance de leMec, metierVrai qui lui fait découvrir l'amour.",
                "Quand leMec, metierVrai romantique, et laMeuf, metierCool de bigCity qui ne croit plus en l'amour, se rencontrent, ils n'ont rien en commun. Et pourtant, ils vont devoir faire équipe pour organiser la parade de Noël de villePaume, etatPaume.",
                "laMeuf, jeune metierCool à bigCity, semble tout avoir&nbsp;: une carrière en constante évolution et un riche fiancé prénommé Alistair. À la mort de sa grand-mère, elle hérite de sa ferme de Noël à villePaume, etatPaume. Venue faire le tour de la propriété pour la vendre au riche promoteur Milton McDollar, elle rencontre le charmant leMec, metierVrai très attaché à la ferme&hellip;",
                "Quand Elena et laMeuf, sœurs jumelles se retrouvent invitées à des soirées de Noël qui ne les intéressent pas, elles décident d'échanger leur place pendant les fêtes. Les leçons qu'elles vont apprendre vont changer leurs vies à jamais.",
                "laMeuf vit les pires vacances de sa vie&nbsp;: son fiancé vient de la plaquer et une grève aérienne l'oblige à prolonger ses vacances à villePaume, etatPaume. Pour ne rien arranger, l'hôtel de luxe qu'elle avait réservé est plein, et elle doit partager une auberge rustique avec leMec, metierVrai bourru."
        };

        private static String draw(String[] array) {
            return array[(int) (Math.random() * array.length)];
        }

        static String generateStory() {
            String storyFull = draw(histoire)
                    .replace("laMeuf", draw(nomMeuf))
                    .replace("leMec", draw(nomMec))
                    .replace("etatPaume", draw(etat))
                    .replace("villePaume", draw(petiteVille))
                    .replace("bigCity", draw(grandeVille))
                    .replace("metierCool", draw(metierMeuf))
                    .replace("metierVrai", draw(metierMec));

            storyFull = StringEscapeUtils.unescapeHtml4(storyFull);
            String titleFull = draw(titre);
            int posterIndex = (int) (Math.random() * 83);

            HashMap<String, String> attachment = new HashMap<>();
            attachment.put("fallback", "**" + titleFull + "**\n" + storyFull);
            attachment.put("title", titleFull);
            attachment.put("text", storyFull);
            attachment.put("image_url", "http://lunatopia.fr/media/pages/blog/films-de-noel/" + posterIndex + ".jpg");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("response_type", "in_channel");
            jsonObject.put("attachments", Collections.singletonList(attachment));

            return jsonObject.toString();
        }
    }
}
