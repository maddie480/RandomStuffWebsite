package com.max480.randomstuff.gae.discord.timezonebot;

import com.max480.randomstuff.gae.ConnectionUtils;
import com.max480.randomstuff.gae.SecretConstants;
import com.max480.randomstuff.gae.discord.DiscordProtocolHandler;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This is the API that makes Timezone Bot run.
 */
@WebServlet(name = "TimezoneBot", urlPatterns = {"/discord/timezone-bot"}, loadOnStartup = 5)
public class InteractionManager extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(InteractionManager.class);

    // offset timezone database
    private static Map<String, String> TIMEZONE_MAP;
    private static Map<String, String> TIMEZONE_FULL_NAMES;
    private static Map<String, List<String>> TIMEZONE_CONFLICTS;

    private static final AtomicLong lastTimezoneDBRequest = new AtomicLong(0);

    private static class UserTimezone implements Serializable {
        @Serial
        private static final long serialVersionUID = 561851525612831863L;

        public String timezoneName;
        public ZonedDateTime expiresAt;
    }

    private static Map<Pair<Long, Long>, UserTimezone> database = new HashMap<>();


    @Override
    public void init() {
        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(Paths.get("/shared/discord-bots/timezone-bot-lite/user-timezones.ser")))) {
            populateTimezones();

            database = (Map<Pair<Long, Long>, UserTimezone>) is.readObject();
            log.debug("Loaded " + database.size() + " timezones.");
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }

        List<Pair<Long, Long>> toDelete = new ArrayList<>();
        for (Map.Entry<Pair<Long, Long>, UserTimezone> entry : database.entrySet()) {
            if (entry.getValue().expiresAt.isBefore(ZonedDateTime.now())) {
                toDelete.add(entry.getKey());
            }
        }

        if (!toDelete.isEmpty()) {
            for (Pair<Long, Long> entry : toDelete) {
                log.warn("Deleting timezone entry {} because it expired!", entry);
                database.remove(entry);
            }

            try {
                saveDatabase();
            } catch (IOException e) {
                log.error("Could not save cleaned up database!", e);
            }
        }
    }

    private static void populateTimezones() throws IOException {
        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(Paths.get("/shared/discord-bots/timezone-bot-lite/timezone-name-data.ser")))) {
            TIMEZONE_MAP = (Map<String, String>) is.readObject();
            TIMEZONE_FULL_NAMES = (Map<String, String>) is.readObject();
            TIMEZONE_CONFLICTS = (Map<String, List<String>>) is.readObject();

            log.info("Time zone offsets: " + TIMEZONE_MAP.size() + ", time zone full names: " + TIMEZONE_FULL_NAMES.size() + ", zone conflicts: " + TIMEZONE_CONFLICTS.size());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject data = DiscordProtocolHandler.validateRequest(req, resp, SecretConstants.TIMEZONE_BOT_PUBLIC_KEY);
        if (data == null) return;

        log.debug("Guild {} used the Timezone Bot!", data.getString("guild_id"));

        String locale = data.getString("locale");

        try {
            if (TIMEZONE_MAP == null) {
                populateTimezones();
            }

            if (data.getInt("type") == 4) {
                // autocomplete
                commandAutocomplete(data, locale, resp);
            } else if (data.getInt("type") == 3) {
                if (data.getJSONObject("data").getInt("component_type") == 2) {
                    int page = Integer.parseInt(data.getJSONObject("data").getString("custom_id").split("\\|")[0]);
                    String action = data.getJSONObject("data").getString("custom_id").split("\\|")[1];

                    long serverId = Long.parseLong(data.getString("guild_id"));
                    if ("list".equals(action)) {
                        listTimezones(response -> resp.getWriter().write(response), serverId, page, locale, true);
                    } else {
                        cleanupInvalidUsers(data, resp, serverId, page, locale);
                    }
                } else {
                    // used a combo box
                    String timezoneFormat = data.getJSONObject("data").getJSONArray("values").getString(0);

                    JSONObject response = new JSONObject();
                    response.put("type", 7); // edit the original response

                    JSONObject responseData = new JSONObject();
                    responseData.put("content", timezoneFormat);
                    responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
                    responseData.put("flags", 1 << 6); // ephemeral
                    response.put("data", responseData);

                    log.debug("Responding with: " + response.toString(2));
                    resp.getWriter().write(response.toString());
                }
            } else {
                // slash command invocation OR user command
                String commandName = data.getJSONObject("data").getString("name");
                long serverId = Long.parseLong(data.getString("guild_id"));
                long memberId = Long.parseLong(data.getJSONObject("member").getJSONObject("user").getString("id"));

                switch (commandName) {
                    case "set-timezone" ->
                            defineUserTimezone(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale);
                    case "detect-timezone" -> sendDetectTimezoneLink(resp, serverId, memberId, locale);
                    case "remove-timezone" -> removeUserTimezone(resp, serverId, memberId, locale);
                    case "discord-timestamp" ->
                            giveDiscordTimestamp(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale);
                    case "time-for" ->
                            giveTimeForOtherUser(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getLong("value"), locale);
                    case "world-clock" ->
                            giveTimeForOtherPlace(resp, serverId, memberId, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), locale);
                    case "list-timezones" ->
                            listTimezones(response -> resp.getWriter().write(response), serverId, 0, locale, false);
                    case "Get Local Time" ->
                            giveTimeForOtherUser(resp, serverId, memberId, data.getJSONObject("data").getLong("target_id"), locale);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred!", e);
            respondPrivately(resp, localizeMessage(locale,
                    ":x: An unexpected error occurred. Reach out at <https://discord.gg/PdyfMaq9Vq> if this keeps happening!",
                    ":x: Une erreur inattendue est survenue. Signale-la sur <https://discord.gg/PdyfMaq9Vq> si ça continue à arriver !"));
        }
    }

    private static String getTimezoneFor(long serverId, long memberId) throws IOException {
        UserTimezone userTimezone = database.get(Pair.of(serverId, memberId));
        if (userTimezone == null) return null;

        userTimezone.expiresAt = ZonedDateTime.now().plusYears(1);
        saveDatabase();

        return userTimezone.timezoneName;
    }

    private static void setTimezoneFor(long serverId, long memberId, String timezone) throws IOException {
        UserTimezone userTimezone = new UserTimezone();
        userTimezone.timezoneName = timezone;
        userTimezone.expiresAt = ZonedDateTime.now().plusYears(1);
        database.put(Pair.of(serverId, memberId), userTimezone);
        saveDatabase();
    }

    private static boolean deleteTimezoneFor(long serverId, long memberId) throws IOException {
        UserTimezone removed = database.remove(Pair.of(serverId, memberId));
        saveDatabase();
        return removed != null;
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

        log.debug("Responding with: " + response.toString(2));
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
                .toList();

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
                .toList();

        // look up conflicting timezone names, showing all possibilities
        List<JSONObject> matchingTimezoneConflictNames = TIMEZONE_CONFLICTS.entrySet().stream()
                .filter(tz -> tz.getKey().toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                .flatMap(tz -> tz.getValue().stream()
                        .map(tzValue -> mapToChoice(tzValue + " (" + tz.getKey() + ")", TIMEZONE_MAP.get(tzValue), locale)))
                .toList();

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
                "To figure out your timezone, visit <https://maddie480.ovh/discord-bots/timezone-bot/detect-timezone>.",
                "Pour déterminer ton fuseau horaire, consulte <https://maddie480.ovh/discord-bots/timezone-bot/detect-timezone>."));
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
            log.info("User " + serverId + " / " + memberId + " now has timezone " + timezoneParam);

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
            log.warn("Could not parse timezone " + timezoneParam);

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
                                "To figure out your timezone, visit <https://maddie480.ovh/discord-bots/timezone-bot/detect-timezone>.",
                        ":x: Le fuseau horaire que tu as donné n'a pas été reconnu.\n" +
                                "Pour déterminer ton fuseau horaire, consulte <https://maddie480.ovh/discord-bots/timezone-bot/detect-timezone>."));
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
                    """
                            :x: The date you gave could not be parsed!
                            Make sure you followed the format `YYYY-MM-dd hh:mm:ss`. For example: `2020-10-01 15:42:00`
                            You can omit part of the date (or omit it entirely if you want today), and the seconds if you don't need that.""",
                    """
                            :x: Je n'ai pas compris la date que tu as donnée !
                            Assure-toi que tu as suivi le format `YYYY-MM-dd hh:mm:ss`. Par exemple : `2020-10-01 15:42:00`
                            Tu peux enlever une partie de la date (ou l'enlever complètement pour obtenir le _timestamp_ d'aujourd'hui) et les secondes si tu n'en as pas besoin."""));
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

            log.debug("Responding with: " + response.toString(2));
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
        String userTimezone = getTimezoneFor(serverId, memberId);

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
                    "The current time for <@" + otherMemberId + "> is **" + nowForUser.format(format) + "** " +
                            "(" + getOffsetAndDifference(timezoneName, userTimezone, locale) + ").",
                    "Pour <@" + otherMemberId + ">, l'horloge affiche **" + nowForUser.format(formatFr) + "** " +
                            "(" + getOffsetAndDifference(timezoneName, userTimezone, locale) + ")."));
        }
    }

    private static void giveTimeForOtherPlace(HttpServletResponse event, long serverId, long memberId, String place, String locale) throws IOException {
        String userTimezone = getTimezoneFor(serverId, memberId);

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
            HttpURLConnection osm = ConnectionUtils.openConnectionWithTimeout("https://nominatim.openstreetmap.org/search.php?" +
                    "q=" + URLEncoder.encode(place, StandardCharsets.UTF_8) +
                    "&accept-language=" + ("fr".equals(locale) ? "fr" : "en") +
                    "&limit=1&format=jsonv2");

            JSONArray osmResults;
            try (InputStream is = ConnectionUtils.connectionToInputStream(osm)) {
                osmResults = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
            }

            if (osmResults.isEmpty()) {
                log.info("Place '" + place + "' was not found by OpenStreetMap!");
                respondPrivately(event, localizeMessage(locale,
                        ":x: This place was not found!",
                        ":x: Ce lieu n'a pas été trouvé !"));
            } else {
                double latitude = osmResults.getJSONObject(0).getFloat("lat");
                double longitude = osmResults.getJSONObject(0).getFloat("lon");
                String name = osmResults.getJSONObject(0).getString("display_name");

                log.debug("Result for place '" + place + "': '" + name + "', latitude " + latitude + ", longitude " + longitude);

                JSONObject timezoneDBResult;
                try (InputStream is = ConnectionUtils.openStreamWithTimeout("https://api.timezonedb.com/v2.1/get-time-zone?key="
                        + SecretConstants.TIMEZONEDB_API_KEY + "&format=json&by=position&lat=" + latitude + "&lng=" + longitude)) {

                    timezoneDBResult = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                }

                if (timezoneDBResult.getString("status").equals("OK")) {
                    ZoneId zoneId;
                    try {
                        zoneId = ZoneId.of(timezoneDBResult.getString("zoneName"));
                    } catch (DateTimeException e) {
                        log.info("Zone ID '" + timezoneDBResult.getString("zoneName") + "' was not recognized! Falling back to UTC offset.");
                        zoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofTotalSeconds(timezoneDBResult.getInt("gmtOffset")));
                    }
                    log.debug("Timezone of '" + name + "' is: " + zoneId);

                    ZonedDateTime nowAtPlace = ZonedDateTime.now(zoneId);
                    DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.ENGLISH);
                    DateTimeFormatter formatFr = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.FRENCH);

                    respondPrivately(event, localizeMessage(locale,
                            "The current time in **" + name + "** is **" + nowAtPlace.format(format) + "** " +
                                    "(" + getOffsetAndDifference(zoneId.toString(), userTimezone, locale) + ").",
                            "A **" + name + "**, l'horloge affiche **" + nowAtPlace.format(formatFr) + "** " +
                                    "(" + getOffsetAndDifference(zoneId.toString(), userTimezone, locale) + ")."));
                } else {
                    log.info("Coordinates (" + latitude + ", " + longitude + ") were not found by TimeZoneDB!");
                    respondPrivately(event, localizeMessage(locale,
                            ":x: This place was not found!",
                            ":x: Ce lieu n'a pas été trouvé !"));
                }
            }
        } catch (IOException | JSONException | InterruptedException e) {
            log.error("Error while querying timezone for " + place + ": " + e);
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

    private void listTimezones(IOConsumer<String> respond, long serverId, int page, String locale, boolean edit) throws IOException {
        // list all members from the server, and group them by UTC offset
        Map<Integer, Set<Long>> peopleByUtcOffset = database.entrySet().stream()
                .filter(entry -> entry.getKey().getLeft() == serverId)
                .collect(Collectors.toMap(
                        // key = UTC offset in minutes
                        entry -> {
                            ZoneId zone = ZoneId.of(entry.getValue().timezoneName);
                            ZonedDateTime now = ZonedDateTime.now(zone);
                            return now.getOffset().getTotalSeconds() / 60;
                        },
                        // value = list of member IDs, which we initialize with a singleton set
                        entry -> Collections.singleton(entry.getKey().getRight()),
                        // merge of 2 values with the same keys = just merge the 2 sets
                        (list1, list2) -> {
                            Set<Long> merged = new TreeSet<>(list1);
                            merged.addAll(list2);
                            return merged;
                        }
                ));

        if (peopleByUtcOffset.isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("type", 4); // response in channel

            JSONObject responseData = new JSONObject();
            responseData.put("content", localizeMessage(locale,
                    ":x: Nobody set up their timezone with `/set-timezone` on this server!",
                    ":x: Personne n'a configuré son fuseau horaire avec `/set-timezone` sur ce serveur !"));
            responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
            responseData.put("flags", 1 << 6); // ephemeral
            response.put("data", responseData);

            respond.accept(response.toString());
        } else {
            List<String> timezonesList = generateTimezonesList(peopleByUtcOffset, locale);
            page = Math.min(page, timezonesList.size() - 1);

            JSONObject response = new JSONObject();
            response.put("type", edit ? 7 : 4);

            JSONObject responseData = new JSONObject();
            responseData.put("content", timezonesList.get(page));
            responseData.put("flags", 1 << 6); // ephemeral
            response.put("data", responseData);

            JSONArray components = new JSONArray();
            responseData.put("components", components);

            if (timezonesList.size() > 1) {
                JSONObject actionRow = new JSONObject();
                components.put(actionRow);
                actionRow.put("type", 1);

                JSONArray rowComponents = new JSONArray();
                actionRow.put("components", rowComponents);

                // previous page
                JSONObject previousPage = new JSONObject();
                rowComponents.put(previousPage);

                previousPage.put("type", 2);
                previousPage.put("style", 1);
                previousPage.put("disabled", page == 0);
                previousPage.put("custom_id", (page - 1) + "|list");

                JSONObject arrowLeftEmoji = new JSONObject("{\"id\": null}");
                previousPage.put("emoji", arrowLeftEmoji);
                arrowLeftEmoji.put("name", "⬅");

                // next page
                JSONObject nextPage = new JSONObject();
                rowComponents.put(nextPage);

                nextPage.put("type", 2);
                nextPage.put("style", 1);
                nextPage.put("disabled", page == timezonesList.size() - 1);
                nextPage.put("custom_id", (page + 1) + "|list");

                JSONObject arrowRightEmoji = new JSONObject("{\"id\": null}");
                nextPage.put("emoji", arrowRightEmoji);
                arrowRightEmoji.put("name", "➡");
            }

            {
                JSONObject actionRow = new JSONObject();
                components.put(actionRow);
                actionRow.put("type", 1);

                JSONArray rowComponents = new JSONArray();
                actionRow.put("components", rowComponents);

                JSONObject button = new JSONObject();
                rowComponents.put(button);

                button.put("type", 2);
                button.put("style", 2);
                button.put("label", localizeMessage(locale, "Clean up invalid users", "Nettoyer utilisateurs invalides"));
                button.put("custom_id", page + "|cleanup");

                JSONObject emojiObject = new JSONObject("{\"id\": null}");
                button.put("emoji", emojiObject);
                emojiObject.put("name", "\uD83E\uDDF9"); // broom
            }

            log.debug("Responding with: " + response.toString(2));
            respond.accept(response.toString());
        }
    }

    private static List<String> generateTimezonesList(Map<Integer, Set<Long>> people, String locale) {
        List<String> pages = new ArrayList<>();

        StringBuilder currentPage = new StringBuilder();
        for (Map.Entry<Integer, Set<Long>> peopleInTimezone : people.entrySet()) {
            int offsetMinutes = peopleInTimezone.getKey();

            OffsetDateTime now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.ofTotalSeconds(offsetMinutes * 60));

            StringBuilder header = new StringBuilder();

            // Discord has nice emoji for every 30 minutes, so let's use them :p
            int hour = now.getHour();
            if (hour == 0) hour = 12;
            if (hour > 12) hour -= 12;

            header.append("**:clock").append(hour);

            if (now.getMinute() >= 30) {
                header.append("30");
            }

            header.append(": ");

            header.append(formatTimezoneName(offsetMinutes))
                    .append(" (").append(now.format(DateTimeFormatter.ofPattern("fr".equals(locale) ? "H:mm" : "h:mma")).toLowerCase(Locale.ROOT)).append(")");

            header.append("**");
            header.append("\n");

            if (currentPage.length() + header.length() + 25 > 2000) {
                // we cannot fit the new header and at least one user in the current page! create a new one.
                pages.add(currentPage.toString().trim());
                currentPage = new StringBuilder();
            }

            currentPage.append(header);

            for (Long member : peopleInTimezone.getValue()) {
                if (currentPage.length() + 25 > 2000) {
                    // create a new page and repeat the header on it!
                    pages.add(currentPage.toString().trim());
                    currentPage = new StringBuilder();
                    currentPage.append(header);
                }

                currentPage.append("<@").append(member).append(">\n");
            }

            currentPage.append("\n");
        }

        if (currentPage.toString().trim().length() > 0) {
            pages.add(currentPage.toString().trim());
        }

        return pages;
    }

    private static String formatTimezoneName(int zoneOffset) {
        int hours = zoneOffset / 60;
        int minutes = Math.abs(zoneOffset) % 60;
        DecimalFormat twoDigits = new DecimalFormat("00");
        return "UTC" + (hours < 0 ? "-" : "+") + twoDigits.format(Math.abs(hours)) + ":" + twoDigits.format(minutes);
    }

    private void cleanupInvalidUsers(JSONObject data, HttpServletResponse resp, long serverId, int page, String locale) throws IOException {
        final List<Long> usersToCleanUp = new ArrayList<>();

        for (String line : data.getJSONObject("message").getString("content").split("\n")) {
            if (line.startsWith("<@")) {
                long id = Long.parseLong(line.substring(2, line.length() - 1));

                boolean found = false;
                for (Object o : data.getJSONObject("message").getJSONArray("mentions")) {
                    if (Long.parseLong(((JSONObject) o).getString("id")) == id) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    usersToCleanUp.add(id);
                }
            }
        }

        if (usersToCleanUp.isEmpty()) {
            respondPrivately(resp, localizeMessage(locale,
                    "All users seem valid to me. :thinking:",
                    "Tous les utilisateurs m'ont l'air valides. :thinking:"));
        } else {
            // respond now with a loading message (Discord only displays loading if responding in a new message aaaa)
            JSONObject response = new JSONObject();
            response.put("type", 7); // update message

            JSONObject responseData = new JSONObject();
            responseData.put("content", localizeMessage(locale,
                    ":broom: Cleaning up invalid users, please wait...",
                    ":broom: Nettoyage des utilisateurs invalides en cours, veuillez patienter..."));
            responseData.put("components", new JSONArray());
            response.put("data", responseData);

            log.debug("Responding with: " + response.toString(2));
            resp.getWriter().write(response.toString());

            // delete the invalid users asynchronously, and call Discord when this is done
            new Thread(() -> {
                try {
                    for (long userId : usersToCleanUp) {
                        deleteTimezoneFor(serverId, userId);
                    }

                    listTimezones(r -> {
                        JSONObject responseParsed = new JSONObject(r);
                        respondDeferred(data.getString("token"), responseParsed.getJSONObject("data").toString());
                    }, serverId, page, locale, true);
                } catch (Exception e) {
                    log.error("Failed to asynchronously clean up user ids!", e);
                }
            }).start();
        }
    }

    private static String getOffsetAndDifference(String consideredTimezone, String userTimezone, String locale) {
        ZoneId zone = ZoneId.of(consideredTimezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        int consideredTimezoneOffset = now.getOffset().getTotalSeconds() / 60;

        if (userTimezone == null) {
            return formatTimezoneName(consideredTimezoneOffset);
        } else {
            zone = ZoneId.of(userTimezone);
            now = ZonedDateTime.now(zone);
            int userTimezoneOffset = now.getOffset().getTotalSeconds() / 60;

            int timeDifference = consideredTimezoneOffset - userTimezoneOffset;

            if (timeDifference == 0) {
                return formatTimezoneName(consideredTimezoneOffset) + ", " + localizeMessage(locale, "same time as you", "même heure que toi");
            } else {
                String comment;
                int hours = Math.abs(timeDifference / 60);
                int minutes = Math.abs(timeDifference % 60);

                if (minutes == 0) {
                    comment = hours + " " + localizeMessage(locale, "hour", "heure") + (hours == 1 ? "" : "s");
                } else {
                    comment = hours + ":" + new DecimalFormat("00").format(minutes);
                }

                if (timeDifference < 0) {
                    comment += " " + localizeMessage(locale, "behind you", "de retard sur toi");
                } else {
                    comment += " " + localizeMessage(locale, "ahead of you", "d'avance sur toi");
                }

                return formatTimezoneName(consideredTimezoneOffset) + ", " + comment;
            }
        }
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

        log.debug("Responding with: " + response.toString(2));
        responseStream.getWriter().write(response.toString());
    }

    private static void respondDeferred(String interactionToken, String message) throws IOException {
        String url = "https://discord.com/api/v10/webhooks/" + SecretConstants.TIMEZONE_BOT_CLIENT_ID + "/" + interactionToken + "/messages/@original";

        // Apache HttpClient because PATCH does not exist according to HttpURLConnection
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch(new URI(url));
            httpPatch.setHeader("User-Agent", "DiscordBot (https://maddie480.ovh/discord-bots/#timezone-bot, 1.0)");
            httpPatch.setEntity(new StringEntity(message, ContentType.APPLICATION_JSON));
            CloseableHttpResponse httpResponse = httpClient.execute(httpPatch);

            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                log.error("Discord responded with " + httpResponse.getStatusLine().getStatusCode() + " to our edit request!");
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static String localizeMessage(String locale, String english, String french) {
        if ("fr".equals(locale)) {
            return french;
        }

        return english;
    }

    private static void saveDatabase() throws IOException {
        try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(Paths.get("/shared/discord-bots/timezone-bot-lite/user-timezones.ser")))) {
            os.writeObject(database);
        }
    }
}
