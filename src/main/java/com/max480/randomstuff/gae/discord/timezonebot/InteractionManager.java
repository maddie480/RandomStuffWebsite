package com.max480.randomstuff.gae.discord.timezonebot;

import com.google.appengine.api.datastore.*;
import com.max480.randomstuff.gae.CloudStorageUtils;
import com.max480.randomstuff.gae.ConnectionUtils;
import com.max480.randomstuff.gae.SecretConstants;
import com.max480.randomstuff.gae.discord.DiscordProtocolHandler;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This is the API that makes Timezone Bot run.
 */
@WebServlet(name = "TimezoneBot", urlPatterns = {"/discord/timezone-bot"}, loadOnStartup = 6)
public class InteractionManager extends HttpServlet {
    private static final Logger logger = Logger.getLogger("InteractionManager");
    private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // offset timezone database
    private static Map<String, String> TIMEZONE_MAP;
    private static Map<String, String> TIMEZONE_FULL_NAMES;
    private static Map<String, List<String>> TIMEZONE_CONFLICTS;

    private static final AtomicLong lastTimezoneDBRequest = new AtomicLong(0);

    @Override
    public void init() {
        try {
            populateTimezones();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e);
        }
    }

    private static void populateTimezones() throws IOException {
        try (ObjectInputStream is = new ObjectInputStream(CloudStorageUtils.getCloudStorageInputStream("timezone_name_data.ser"))) {
            TIMEZONE_MAP = (Map<String, String>) is.readObject();
            TIMEZONE_FULL_NAMES = (Map<String, String>) is.readObject();
            TIMEZONE_CONFLICTS = (Map<String, List<String>>) is.readObject();

            logger.info("Time zone offsets: " + TIMEZONE_MAP.size() + ", time zone full names: " + TIMEZONE_FULL_NAMES.size() + ", zone conflicts: " + TIMEZONE_CONFLICTS.size());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject data = DiscordProtocolHandler.validateRequest(req, resp, SecretConstants.TIMEZONE_BOT_PUBLIC_KEY);
        if (data == null) return;

        String locale = data.getString("locale");

        try {
            if (TIMEZONE_MAP == null) {
                populateTimezones();
            }

            if (data.getInt("type") == 4) {
                // autocomplete
                commandAutocomplete(data, locale, resp);
            } else if (data.getInt("type") == 3) {
                // used a combo box
                String timezoneFormat = data.getJSONObject("data").getJSONArray("values").getString(0);

                JSONObject response = new JSONObject();
                response.put("type", 7); // edit the original response

                JSONObject responseData = new JSONObject();
                responseData.put("content", timezoneFormat);
                responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
                responseData.put("flags", 1 << 6); // ephemeral
                response.put("data", responseData);

                logger.fine("Responding with: " + response.toString(2));
                resp.getWriter().write(response.toString());
            } else {
                // slash command invocation OR user command
                String commandName = data.getJSONObject("data").getString("name");
                long serverId = Long.parseLong(data.getString("guild_id"));
                long memberId = Long.parseLong(data.getJSONObject("member").getJSONObject("user").getString("id"));

                switch (commandName) {
                    case "set-timezone":
                        defineUserTimezone(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale);
                        break;

                    case "detect-timezone":
                        sendDetectTimezoneLink(resp, serverId, memberId, locale);
                        break;

                    case "remove-timezone":
                        removeUserTimezone(resp, serverId, memberId, locale);
                        break;

                    case "discord-timestamp":
                        giveDiscordTimestamp(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale);
                        break;

                    case "time-for":
                        giveTimeForOtherUser(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getLong("value"), locale);
                        break;

                    case "world-clock":
                        giveTimeForOtherPlace(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale);
                        break;

                    case "Get Local Time":
                        giveTimeForOtherUser(resp, serverId, memberId, data.getJSONObject("data").getLong("target_id"), locale);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("An unexpected error occurred: " + e);
            respondPrivately(resp, localizeMessage(locale,
                    ":x: An unexpected error occurred. Reach out at <https://discord.gg/59ztc8QZQ7> if this keeps happening!",
                    ":x: Une erreur inattendue est survenue. Signale-la sur <https://discord.gg/59ztc8QZQ7> si ça continue à arriver !"));
        }
    }

    private static String getTimezoneFor(long serverId, long memberId) {
        try {
            Entity entity = datastore.get(KeyFactory.createKey("timezoneBotData", serverId + "/" + memberId));
            entity.setProperty("expiresAt", new Date(ZonedDateTime.now().plusYears(1).toEpochSecond() * 1000L));
            datastore.put(entity);
            return (String) entity.getProperty("timezoneName");
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    private static void setTimezoneFor(long serverId, long memberId, String timezone) {
        Entity entity = new Entity("timezoneBotData", serverId + "/" + memberId);
        entity.setProperty("serverId", serverId);
        entity.setProperty("memberId", memberId);
        entity.setProperty("timezoneName", timezone);
        entity.setProperty("expiresAt", new Date(ZonedDateTime.now().plusYears(1).toEpochSecond() * 1000L));
        datastore.put(entity);
    }

    private static boolean deleteTimezoneFor(long serverId, long memberId) {
        try {
            Entity entity = datastore.get(KeyFactory.createKey("timezoneBotData", serverId + "/" + memberId));
            datastore.delete(entity.getKey());
            return true;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    private void commandAutocomplete(JSONObject interaction, String locale, HttpServletResponse resp) throws IOException {
        String partialName = interaction.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value");

        JSONObject response = new JSONObject();
        response.put("type", 8); // autocomplete result

        JSONObject responseData = new JSONObject();
        JSONArray choices = new JSONArray();

        for (JSONObject o : suggestTimezones(partialName, locale)) {
            choices.put(o);
        }

        responseData.put("choices", choices);
        response.put("data", responseData);

        logger.fine("Responding with: " + response.toString(2));
        resp.getWriter().write(response.toString());
    }


    private List<JSONObject> suggestTimezones(String input, String locale) {
        // look up tz database timezones
        List<JSONObject> matchingTzDatabaseTimezones = ZoneId.getAvailableZoneIds().stream()
                .filter(tz -> {
                    if (tz.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                    if (!tz.contains("/")) {
                        return false;
                    }
                    // suggest "Europe/Paris" if the user started typing "Paris"
                    return tz.substring(tz.lastIndexOf("/") + 1).toLowerCase(Locale.ROOT)
                            .startsWith(input.toLowerCase(Locale.ROOT));
                })
                .map(tz -> mapToChoice(tz, tz, locale))
                .collect(Collectors.toList());

        if (input.isEmpty()) {
            // we want to push for tz database timezones, so list them by default!
            return matchingTzDatabaseTimezones.stream()
                    .sorted(Comparator.comparing(tz -> tz.getString("value").toLowerCase(Locale.ROOT)))
                    .limit(25)
                    .collect(Collectors.toList());
        }

        // look up timezone names
        List<JSONObject> matchingTimezoneNames = TIMEZONE_MAP.entrySet().stream()
                .filter(tz -> tz.getKey().toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .map(tz -> {
                    String tzName = tz.getKey();
                    if (TIMEZONE_FULL_NAMES.containsKey(tzName)) {
                        tzName = TIMEZONE_FULL_NAMES.get(tzName) + " (" + tzName + ")";
                    }
                    return mapToChoice(tzName, tz.getValue(), locale);
                })
                .collect(Collectors.toList());

        // look up conflicting timezone names, showing all possibilities
        List<JSONObject> matchingTimezoneConflictNames = TIMEZONE_CONFLICTS.entrySet().stream()
                .filter(tz -> tz.getKey().toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .flatMap(tz -> tz.getValue().stream()
                        .map(tzValue -> mapToChoice(tzValue + " (" + tz.getKey() + ")", TIMEZONE_MAP.get(tzValue), locale)))
                .collect(Collectors.toList());

        List<JSONObject> allChoices = new ArrayList<>(matchingTzDatabaseTimezones);
        allChoices.addAll(matchingTimezoneNames);
        allChoices.addAll(matchingTimezoneConflictNames);

        if (!allChoices.isEmpty()) {
            // send them sorting by alphabetical order and return as many as possible
            return allChoices.stream()
                    .sorted(Comparator.comparing(tz -> tz.getString("name").toLowerCase(Locale.ROOT)))
                    .limit(25)
                    .collect(Collectors.toList());
        } else {
            try {
                // if the timezone is valid, be sure to allow the user to use it!
                return Collections.singletonList(mapToChoice(input, input, locale));
            } catch (DateTimeException e) {
                // no match :shrug:
                return Collections.emptyList();
            }
        }
    }

    private JSONObject mapToChoice(String tzName, String zoneId, String locale) {
        String localTime;
        if ("fr".equals(locale)) {
            localTime = ZonedDateTime.now(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            localTime = ZonedDateTime.now(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase(Locale.ROOT);
        }

        JSONObject o = new JSONObject();
        o.put("name", tzName + " (" + localTime + ")");
        o.put("value", zoneId);
        return o;
    }

    /**
     * Handles the /detect-timezone command: sends the link to the detect timezone page.
     */
    private static void sendDetectTimezoneLink(HttpServletResponse event, long serverId, long memberId, String locale) throws IOException {
        getTimezoneFor(serverId, memberId);

        respondPrivately(event, localizeMessage(locale,
                "To figure out your timezone, visit <https://max480-random-stuff.appspot.com/detect-timezone.html>.",
                "Pour déterminer ton fuseau horaire, consulte <https://max480-random-stuff.appspot.com/detect-timezone.html>."));
    }

    /**
     * Handles the /set-timezone command: saves a new timezone for the given user.
     */
    private void defineUserTimezone(HttpServletResponse event, long serverId, long memberId, String timezoneParam, String locale) throws IOException {
        try {
            // if the user passed for example "EST", convert it to "UTC-5".
            String timezoneOffsetFromName = getIgnoreCase(TIMEZONE_MAP, timezoneParam);
            if (timezoneOffsetFromName != null) {
                timezoneParam = timezoneOffsetFromName;
            }

            // check that the timezone is valid by passing it to ZoneId.of.
            ZonedDateTime localNow = ZonedDateTime.now(ZoneId.of(timezoneParam));

            // save the link
            setTimezoneFor(serverId, memberId, timezoneParam);
            logger.info("User " + serverId + " / " + memberId + " now has timezone " + timezoneParam);

            DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.ENGLISH);
            DateTimeFormatter formatFr = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.FRENCH);
            respondPrivately(event, localizeMessage(locale,
                    ":white_check_mark: Your timezone was saved as **" + timezoneParam + "**.\n" +
                            "The current time in this timezone is **" + localNow.format(format) + "**. " +
                            "If this does not match your local time, type `/detect-timezone` to find the right one.",
                    ":white_check_mark: Ton fuseau horaire a été enregistré : **" + timezoneParam + "**.\n" +
                            "L'heure qu'il est dans ce fuseau horaire est **" + localNow.format(formatFr) + "**. " +
                            "Si cela ne correspond pas à l'heure qu'il est chez toi, tape `/detect-timezone` pour trouver le bon fuseau horaire."));
        } catch (DateTimeException ex) {
            // ZoneId.of blew up so the timezone is probably invalid.
            logger.warning("Could not parse timezone " + timezoneParam);

            List<String> conflictingTimezones = getIgnoreCase(TIMEZONE_CONFLICTS, timezoneParam);
            if (conflictingTimezones != null) {
                respondPrivately(event, localizeMessage(locale,
                        ":x: The timezone **" + timezoneParam + "** is ambiguous! It could mean one of those: _"
                                + String.join("_, _", conflictingTimezones) + "_.\n" +
                                "Repeat the command with the timezone full name!",
                        ":x: Le fuseau horaire **" + timezoneParam + "** est ambigu ! Il peut désigner _"
                                + String.join("_, _", conflictingTimezones) + "_.\n" +
                                "Relance la commande avec le nom complet du fuseau horaire !"));
            } else {
                respondPrivately(event, localizeMessage(locale,
                        ":x: The given timezone was not recognized.\n" +
                                "To figure out your timezone, visit <https://max480-random-stuff.appspot.com/detect-timezone.html>.",
                        ":x: Le fuseau horaire que tu as donné n'a pas été reconnu.\n" +
                                "Pour déterminer ton fuseau horaire, consulte <https://max480-random-stuff.appspot.com/detect-timezone.html>."));
            }
        }
    }

    /**
     * Handles the /remove-timezone command: takes off the timezone and forgets about the user.
     */
    private static void removeUserTimezone(HttpServletResponse event, long serverId, long memberId, String locale) throws IOException {
        if (deleteTimezoneFor(serverId, memberId)) {
            respondPrivately(event, localizeMessage(locale,
                    ":white_check_mark: Your timezone has been removed.",
                    ":white_check_mark: Ton fuseau horaire a été supprimé."));
        } else {
            // user asked for their timezone to be forgotten, but doesn't have a timezone to start with :thonk:
            respondPrivately(event, localizeMessage(locale,
                    ":x: You don't currently have a timezone saved!",
                    ":x: Tu n'as pas de fuseau horaire enregistré !"));
        }
    }

    /**
     * Handles the /discord-timestamp command: gives the Discord timestamp for the given date.
     */
    private static void giveDiscordTimestamp(HttpServletResponse event, long serverId, long memberId, String dateTimeParam, String locale) throws IOException {
        // find the user's timezone.
        String timezoneName = getTimezoneFor(serverId, memberId);

        // if the user has no timezone, we want to use UTC instead!
        String timezoneToUse = timezoneName == null ? "UTC" : timezoneName;

        // take the given date time with the user's timezone (or UTC), then turn it into a timestamp.
        LocalDateTime parsedDateTime = tryParseSuccessively(Arrays.asList(
                // full date
                () -> LocalDateTime.parse(dateTimeParam, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")),
                () -> LocalDateTime.parse(dateTimeParam, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")).withSecond(0),

                // year omitted
                () -> LocalDateTime.parse(LocalDate.now(ZoneId.of(timezoneToUse)).getYear() + "-" + dateTimeParam,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")),
                () -> LocalDateTime.parse(LocalDate.now(ZoneId.of(timezoneToUse)).getYear() + "-" + dateTimeParam,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")).withSecond(0),

                // month omitted
                () -> LocalDateTime.parse(LocalDate.now(ZoneId.of(timezoneToUse)).getYear()
                                + "-" + LocalDate.now(ZoneId.of(timezoneToUse)).getMonthValue()
                                + "-" + dateTimeParam,
                        DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss")),
                () -> LocalDateTime.parse(LocalDate.now(ZoneId.of(timezoneToUse)).getYear()
                                + "-" + LocalDate.now(ZoneId.of(timezoneToUse)).getMonthValue()
                                + "-" + dateTimeParam,
                        DateTimeFormatter.ofPattern("yyyy-M-d H:mm")).withSecond(0),

                // date omitted
                () -> LocalTime.parse(dateTimeParam, DateTimeFormatter.ofPattern("H:mm:ss"))
                        .atDate(LocalDate.now(ZoneId.of(timezoneToUse))),
                () -> LocalTime.parse(dateTimeParam, DateTimeFormatter.ofPattern("H:mm")).withSecond(0)
                        .atDate(LocalDate.now(ZoneId.of(timezoneToUse)))
        ));

        Long timestamp = null;
        if (parsedDateTime == null) {
            respondPrivately(event, localizeMessage(locale,
                    ":x: The date you gave could not be parsed!\nMake sure you followed the format `YYYY-MM-dd hh:mm:ss`. " +
                            "For example: `2020-10-01 15:42:00`\nYou can omit part of the date (or omit it entirely if you want today), and the seconds if you don't need that.",
                    ":x: Je n'ai pas compris la date que tu as donnée !\nAssure-toi que tu as suivi le format `YYYY-MM-dd hh:mm:ss`. " +
                            "Par exemple : `2020-10-01 15:42:00`\nTu peux enlever une partie de la date (ou l'enlever complètement pour obtenir le _timestamp_ d'aujourd'hui) et les secondes si tu n'en as pas besoin."));
        } else {
            timestamp = parsedDateTime.atZone(ZoneId.of(timezoneToUse)).toEpochSecond();
        }

        if (timestamp != null) {
            StringBuilder b = new StringBuilder();
            if (timezoneName == null) {
                // warn the user that we used UTC.
                b.append(localizeMessage(locale,
                        ":warning: You did not set your timezone with `/set-timezone`, so **UTC** was used instead.\n\n",
                        ":warning: Tu n'as pas défini ton fuseau horaire avec `/set-timezone`, donc le fuseau horaire **UTC** sera utilisé à la place.\n\n"));
            }

            // print `<t:timestamp:format>` => <t:timestamp:format> for all available formats.
            b.append(localizeMessage(locale,
                            "Copy-paste one of those tags in your message, and others will see **",
                            "Copie-colle l'un de ces tags dans ton message, et les autres verront **"))
                    .append(dateTimeParam)
                    .append(localizeMessage(locale,
                            "** in their timezone:\n",
                            "** dans leur fuseau horaire :\n"));
            for (char format : new char[]{'t', 'T', 'd', 'D', 'f', 'F', 'R'}) {
                b.append("`<t:").append(timestamp).append(':').append(format)
                        .append(">` :arrow_right: <t:").append(timestamp).append(':').append(format).append(">\n");
            }
            b.append(localizeMessage(locale,
                    "\n\nIf you are on mobile, pick a format for easier copy-pasting:",
                    "\n\nSi tu es sur mobile, choisis un format pour pouvoir le copier-coller plus facilement :"));

            // we want to show a selection menu for the user to pick a format.
            // the idea is that they can get a message with only the tag to copy in it and can copy it on mobile,
            // which is way more handy than selecting part of the full message on mobile.
            ZonedDateTime time = Instant.ofEpochSecond(timestamp).atZone(ZoneId.of(timezoneToUse));

            JSONObject response = new JSONObject();
            response.put("type", 4); // response in channel

            JSONObject responseData = new JSONObject();
            responseData.put("content", b.toString().trim());
            responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
            responseData.put("flags", 1 << 6); // ephemeral
            response.put("data", responseData);

            JSONArray components = new JSONArray();
            responseData.put("components", components);

            JSONObject actionRow = new JSONObject();
            components.put(actionRow);

            actionRow.put("type", 1);

            JSONArray rowComponents = new JSONArray();
            actionRow.put("components", rowComponents);

            JSONObject selectMenu = new JSONObject();
            rowComponents.put(selectMenu);

            selectMenu.put("type", 3);
            selectMenu.put("custom_id", "discord-timestamp");

            JSONArray options = new JSONArray();
            selectMenu.put("options", options);

            if ("fr".equals(locale)) {
                addOption(options, time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)), "<t:" + timestamp + ":t>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.FRENCH)), "<t:" + timestamp + ":T>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)), "<t:" + timestamp + ":d>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)), "<t:" + timestamp + ":D>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("d MMMM yyyy H:mm", Locale.FRENCH)), "<t:" + timestamp + ":f>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy H:mm", Locale.FRENCH)), "<t:" + timestamp + ":F>");
                addOption(options, "Différence par rapport à maintenant", "<t:" + timestamp + ":R>");
            } else {
                addOption(options, time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)), "<t:" + timestamp + ":t>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH)), "<t:" + timestamp + ":T>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH)), "<t:" + timestamp + ":d>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)), "<t:" + timestamp + ":D>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a", Locale.ENGLISH)), "<t:" + timestamp + ":f>");
                addOption(options, time.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.ENGLISH)), "<t:" + timestamp + ":F>");
                addOption(options, "Relative to now", "<t:" + timestamp + ":R>");
            }

            logger.fine("Responding with: " + response.toString(2));
            event.getWriter().write(response.toString());
        }
    }

    private static void addOption(JSONArray optionList, String label, String value) {
        JSONObject object = new JSONObject();
        object.put("label", label);
        object.put("value", value);
        optionList.put(object);
    }

    /**
     * Handles the /time-for command: gives the time it is for another server member.
     */
    private static void giveTimeForOtherUser(HttpServletResponse event, long serverId, long memberId, long otherMemberId, String locale) throws IOException {
        getTimezoneFor(serverId, memberId);

        // find the target user's timezone.
        String timezoneName = getTimezoneFor(serverId, otherMemberId);

        if (timezoneName == null) {
            // the user is not in the database.
            respondPrivately(event, localizeMessage(locale,
                    ":x: <@" + otherMemberId + "> did not configure their timezone.",
                    ":x: <@" + otherMemberId + "> n'a pas configuré son fuseau horaire."));
        } else {
            // format the time and display it!
            ZonedDateTime nowForUser = ZonedDateTime.now(ZoneId.of(timezoneName));
            DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.ENGLISH);
            DateTimeFormatter formatFr = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.FRENCH);

            respondPrivately(event, localizeMessage(locale,
                    "The current time for <@" + otherMemberId + "> is **" + nowForUser.format(format) + "**.",
                    "Pour <@" + otherMemberId + ">, l'horloge affiche **" + nowForUser.format(formatFr) + "**."));
        }
    }

    private static void giveTimeForOtherPlace(HttpServletResponse event, long serverId, long memberId, String place, String locale) throws IOException {
        getTimezoneFor(serverId, memberId);

        try {
            // rate limit: 1 request per second
            synchronized (lastTimezoneDBRequest) {
                long timeToWait = lastTimezoneDBRequest.get() - System.currentTimeMillis() + 1000;
                if (timeToWait > 0) {
                    Thread.sleep(timeToWait);
                }
                lastTimezoneDBRequest.set(System.currentTimeMillis());
            }

            // query OpenStreetMap
            HttpURLConnection osm = (HttpURLConnection) new URL("https://nominatim.openstreetmap.org/search.php?" +
                    "q=" + URLEncoder.encode(place, "UTF-8") +
                    "&accept-language=" + ("fr".equals(locale) ? "fr" : "en") +
                    "&limit=1&format=jsonv2")
                    .openConnection();
            osm.setConnectTimeout(10000);
            osm.setReadTimeout(30000);
            osm.setRequestProperty("User-Agent", "TimezoneBot/1.0 (+https://max480-random-stuff.appspot.com/discord-bots#timezone-bot)");

            JSONArray osmResults;
            try (InputStream is = osm.getInputStream()) {
                osmResults = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
            }

            if (osmResults.isEmpty()) {
                logger.info("Place '" + place + "' was not found by OpenStreetMap!");
                respondPrivately(event, localizeMessage(locale,
                        ":x: This place was not found!",
                        ":x: Ce lieu n'a pas été trouvé !"));
            } else {
                double latitude = osmResults.getJSONObject(0).getFloat("lat");
                double longitude = osmResults.getJSONObject(0).getFloat("lon");
                String name = osmResults.getJSONObject(0).getString("display_name");

                logger.fine("Result for place '" + place + "': '" + name + "', latitude " + latitude + ", longitude " + longitude);

                JSONObject timezoneDBResult;
                try (InputStream is = ConnectionUtils.openStreamWithTimeout(new URL("https://api.timezonedb.com/v2.1/get-time-zone?key="
                        + SecretConstants.TIMEZONEDB_API_KEY + "&format=json&by=position&lat=" + latitude + "&lng=" + longitude))) {

                    timezoneDBResult = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                }

                if (timezoneDBResult.getString("status").equals("OK")) {
                    ZoneId zoneId;
                    try {
                        zoneId = ZoneId.of(timezoneDBResult.getString("zoneName"));
                    } catch (DateTimeException e) {
                        logger.info("Zone ID '" + timezoneDBResult.getString("zoneName") + "' was not recognized! Falling back to UTC offset.");
                        zoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(timezoneDBResult.getInt("gmtOffset")));
                    }
                    logger.fine("Timezone of '" + name + "' is: " + zoneId);

                    ZonedDateTime nowAtPlace = ZonedDateTime.now(zoneId);
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.ENGLISH);
                    DateTimeFormatter formatFr = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.FRENCH);

                    respondPrivately(event, localizeMessage(locale,
                            "The current time in **" + name + "** is **" + nowAtPlace.format(format) + "**.",
                            "A **" + name + "**, l'horloge affiche **" + nowAtPlace.format(formatFr) + "**."));
                } else {
                    logger.info("Coordinates (" + latitude + ", " + longitude + ") were not found by TimeZoneDB!");
                    respondPrivately(event, localizeMessage(locale,
                            ":x: This place was not found!",
                            ":x: Ce lieu n'a pas été trouvé !"));
                }
            }
        } catch (IOException | JSONException | InterruptedException e) {
            logger.severe("Error while querying timezone for " + place + ": " + e);
            respondPrivately(event, localizeMessage(locale,
                    ":x: A technical error occurred.",
                    ":x: Une erreur technique est survenue."));
        }
    }

    /**
     * Tries to parse a date with multiple formats.
     * The given methods should either return the parsed LocalDateTime, or throw a DateTimeParseException.
     *
     * @param formatsToAttempt The methods to call in order to try parsing the date
     * @return The result of the first format in the list that could parse the given date, or null if no format matched
     */
    private static LocalDateTime tryParseSuccessively(List<Supplier<LocalDateTime>> formatsToAttempt) {
        // try all the formats one to one.
        for (Supplier<LocalDateTime> formatToAttempt : formatsToAttempt) {
            try {
                return formatToAttempt.get();
            } catch (DateTimeParseException e) {
                // continue!
            }
        }

        // no format matched!
        return null;
    }

    /**
     * Equivalent to Map.get(key), except the key is case-insensitive.
     *
     * @param map The map to get the element from
     * @param key The element to get
     * @param <T> The type of the values in the map
     * @return The value that was found, or null if no value was found
     */
    private static <T> T getIgnoreCase(Map<String, T> map, String key) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * Responds privately to a slash command, in response to the HTTP request.
     */
    private static void respondPrivately(HttpServletResponse responseStream, String message) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("content", message);
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
        responseData.put("flags", 1 << 6); // ephemeral
        response.put("data", responseData);

        logger.fine("Responding with: " + response.toString(2));
        responseStream.getWriter().write(response.toString());
    }

    private static String localizeMessage(String locale, String english, String french) {
        if ("fr".equals(locale)) {
            return french;
        }

        return english;
    }
}
