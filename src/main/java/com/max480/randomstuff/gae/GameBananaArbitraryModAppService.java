package com.max480.randomstuff.gae;

import com.google.appengine.api.datastore.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.max480.randomstuff.gae.ConnectionUtils.openStreamWithTimeout;
import static java.nio.charset.StandardCharsets.UTF_8;

@WebServlet(name = "GameBananaArbitraryModAppService", urlPatterns = {"/gamebanana/arbitrary-mod-app",
        "/gamebanana/arbitrary-mod-app-settings", "/gamebanana/arbitrary-mod-app-housekeep"})
public class GameBananaArbitraryModAppService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("GameBananaArbitraryModAppService");
    private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d yyyy @ h:mm a O", Locale.ENGLISH);

    public static class ModInfo {
        public String url; // _sProfileUrl
        public String name; // _sName
        public String image; // _aPreviewMedia._aImages[0]._sBaseUrl + _aPreviewMedia._aImages[0]._sFile100
        public String dateAdded; // _tsDateAdded
        public String dateAddedClass;
        public String dateAddedRelative;
        public String dateUpdated; // _tsDateUpdated
        public String dateUpdatedClass;
        public String dateUpdatedRelative;
        public String gameName; // _aGame._sName
        public String gameIcon; // _aGame._sIconUrl
        public String gameUrl; // _aGame._sProfileUrl
        public String submitterAvatar; // _aSubmitter._sAvatarUrl
        public String submitterUrl; // _aSubmitter._sProfileUrl
        public String submitterName; // _aSubmitter._sName
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/gamebanana/arbitrary-mod-app-settings")) {
            getSettingsPage(request, response);
            return;
        }

        if (request.getRequestURI().equals("/gamebanana/arbitrary-mod-app-housekeep")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                housekeep();
            } else {
                // invalid secret
                logger.warning("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        // check that the mandatory _idProfile parameter is present, respond 400 Bad Request otherwise.
        String memberId = request.getParameter("_idProfile");
        if (memberId == null) {
            logger.warning("Bad Request");
            response.setStatus(400);
            return;
        }

        String name, list;
        try {
            Entity dbEntity = datastore.get(KeyFactory.createKey("arbitraryModAppConfiguration", memberId));
            name = (String) dbEntity.getProperty("title");
            list = (String) dbEntity.getProperty("modList");
        } catch (EntityNotFoundException e) {
            // whoops, user doesn't exist yet.
            response.setHeader("Content-Type", "text/html");
            response.getWriter().write("<b>Arbitrary Mod App was not configured!</b>");
            return;
        }

        List<ModInfo> modList = Arrays.stream(list.split(",")).parallel()
                // request all mods the user asked for.
                .map(this::queryModById)
                // take out the ones that errored or were withheld / trashed.
                .filter(o -> o != null && !o.getBoolean("_bIsWithheld") && !o.getBoolean("_bIsTrashed") || o.getBoolean("_bIsPrivate"))
                // sort by descending updated date
                .sorted((o1, o2) -> (int) Math.signum(o2.getLong("_tsDateUpdated") - o1.getLong("_tsDateUpdated")))
                .map(object -> {
                    // map them to a ModInfo object.
                    ModInfo info = new ModInfo();
                    info.url = object.getString("_sProfileUrl");
                    info.name = object.getString("_sName");

                    if (!object.getJSONObject("_aPreviewMedia").getJSONArray("_aImages").isEmpty()) {
                        info.image = object.getJSONObject("_aPreviewMedia").getJSONArray("_aImages").getJSONObject(0).getString("_sBaseUrl")
                                + "/" + object.getJSONObject("_aPreviewMedia").getJSONArray("_aImages").getJSONObject(0).getString("_sFile220");
                    }

                    long dateAdded = object.getLong("_tsDateAdded");
                    long secondsAgo = (System.currentTimeMillis() / 1000) - dateAdded;
                    info.dateAdded = Instant.ofEpochSecond(dateAdded).atZone(ZoneId.of("UTC")).format(format);
                    info.dateAddedClass = computeClass(secondsAgo);
                    info.dateAddedRelative = computeAgo(secondsAgo);

                    long dateUpdated = object.getLong("_tsDateUpdated");
                    if (dateAdded != dateUpdated) {
                        secondsAgo = (System.currentTimeMillis() / 1000) - dateUpdated;
                        info.dateUpdated = Instant.ofEpochSecond(dateUpdated).atZone(ZoneId.of("UTC")).format(format);
                        info.dateUpdatedClass = computeClass(secondsAgo);
                        info.dateUpdatedRelative = computeAgo(secondsAgo);
                    }

                    info.gameName = object.getJSONObject("_aGame").getString("_sName");
                    info.gameIcon = object.getJSONObject("_aGame").getString("_sIconUrl");
                    info.gameUrl = object.getJSONObject("_aGame").getString("_sProfileUrl");

                    info.submitterName = object.getJSONObject("_aSubmitter").getString("_sName");
                    info.submitterAvatar = object.getJSONObject("_aSubmitter").getString("_sAvatarUrl");
                    info.submitterUrl = object.getJSONObject("_aSubmitter").getString("_sProfileUrl");

                    return info;
                })
                .collect(Collectors.toList());

        request.setAttribute("isMax480", "1698143".equals(request.getParameter("_idProfile")));
        request.setAttribute("isLoggedOut", "0".equals(request.getParameter("_idMember")));

        response.setHeader("Content-Type", "application/json");
        request.setAttribute("title", name);
        request.setAttribute("modList", modList);
        request.setAttribute("memberId", memberId);

        request.getRequestDispatcher("/WEB-INF/gamebanana-modlist.jsp").forward(request, response);
    }

    private JSONObject queryModById(String modId) {
        try (InputStream is = openStreamWithTimeout(new URL("https://gamebanana.com/apiv8/Mod/" + modId +
                "?_csvProperties=_sProfileUrl,_sName,_aPreviewMedia,_tsDateAdded,_tsDateUpdated,_aGame,_aSubmitter,_bIsWithheld,_bIsTrashed,_bIsPrivate"))) {
            return new JSONObject(IOUtils.toString(is, UTF_8));
        } catch (IOException e) {
            logger.severe("Could not retrieve mod by ID!" + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private void getSettingsPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // check that the mandatory _idMember parameter is present, respond 400 Bad Request otherwise.
        String memberId = request.getParameter("_idMember");
        if (memberId == null) {
            logger.warning("Bad Request");
            response.setStatus(400);
            return;
        }

        request.setAttribute("invalidKey", false);
        request.setAttribute("tooManyMods", false);
        request.setAttribute("appDisabled", false);
        request.setAttribute("invalidMods", false);
        request.setAttribute("saved", false);
        request.setAttribute("typedKey", "");
        request.setAttribute("initialKey", "");

        try {
            Entity dbEntity = datastore.get(KeyFactory.createKey("arbitraryModAppConfiguration", memberId));
            request.setAttribute("title", dbEntity.getProperty("title"));
            request.setAttribute("modList", dbEntity.getProperty("modList"));
            request.setAttribute("isInDatabase", true);
        } catch (EntityNotFoundException e) {
            // whoops, user doesn't exist yet.
            request.setAttribute("title", "");
            request.setAttribute("modList", "");
            request.setAttribute("isInDatabase", false);
        }

        request.getRequestDispatcher("/WEB-INF/gamebanana-modlist-settings.jsp").forward(request, response);
    }

    private String computeClass(long secondsAgo) {
        if (secondsAgo < 300) {
            return "LessThan5minsOld";
        } else if (secondsAgo < 1800) {
            return "LessThan30minsOld";
        } else if (secondsAgo < 3600) {
            return "LessThan1HourOld";
        } else if (secondsAgo < 14400) {
            return "LessThan4HoursOld";
        } else { // GB seems to apply OlderThan1Day to stuff that is just older than 4 hours, so...
            return "OlderThan1Day";
        }
    }

    private String computeAgo(long secondsAgo) {
        if (secondsAgo < 60) {
            return secondsAgo + "s";
        } else if (secondsAgo < 3600) {
            return (secondsAgo / 60) + "m";
        } else if (secondsAgo < 86400) {
            return (secondsAgo / 3600) + "hr";
        } else if (secondsAgo < 2592000) {
            return (secondsAgo / 86400) + "d";
        } else if (secondsAgo < 31536000) {
            return (secondsAgo / 2592000) + "mo";
        } else {
            return (secondsAgo / 31536000) + "y";
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getRequestURI().equals("/gamebanana/arbitrary-mod-app-settings")) {
            // Method Not Allowed: /gamebanana/arbitrary-mod-app only supports GET
            logger.warning("Method Not Allowed");
            response.setStatus(405);
            return;
        }

        // check that the mandatory _idMember parameter is present, respond 400 Bad Request otherwise.
        String memberId = request.getParameter("_idMember");
        if (memberId == null) {
            logger.warning("Bad Request");
            response.setStatus(400);
            return;
        }

        Entity dbEntity;
        String newKey = null;
        try {
            dbEntity = datastore.get(KeyFactory.createKey("arbitraryModAppConfiguration", memberId));
            request.setAttribute("initialKey", "");
            request.setAttribute("isInDatabase", true);
        } catch (EntityNotFoundException e) {
            // we must create the user with a random key.
            newKey = UUID.randomUUID().toString();
            dbEntity = new Entity(KeyFactory.createKey("arbitraryModAppConfiguration", memberId));
            dbEntity.setProperty("key", newKey);
            request.setAttribute("isInDatabase", false);
        }

        // never trust the frontend.
        if (StringUtils.isEmpty(request.getParameter("title")) ||
                (newKey == null && StringUtils.isEmpty(request.getParameter("key"))) ||
                request.getParameter("modlist") == null ||
                !request.getParameter("modlist").matches("([0-9]+,)*[0-9]+")) {

            // the user must have bypassed the HTML5 validations, so just send them an answer just like if they did a GET request.
            logger.warning("POST was given up due to invalid parameters.");
            getSettingsPage(request, response);
            return;
        }

        // default attributes
        request.setAttribute("invalidKey", false);
        request.setAttribute("tooManyMods", false);
        request.setAttribute("appDisabled", false);
        request.setAttribute("invalidMods", false);
        request.setAttribute("saved", false);
        request.setAttribute("initialKey", "");

        // fill the form with the values previously typed in
        request.setAttribute("typedKey", newKey == null ? request.getParameter("key") : "");
        request.setAttribute("title", request.getParameter("title"));
        request.setAttribute("modList", request.getParameter("modlist"));

        if (newKey == null && !request.getParameter("key").equals(dbEntity.getProperty("key"))) {
            logger.warning("Invalid key");
            request.setAttribute("invalidKey", true);
        } else if (request.getParameter("modlist").split(",").length > 50) {
            logger.warning("Too many mods");
            request.setAttribute("tooManyMods", true);
        } else if (!getAllUsers().contains(memberId)) {
            logger.warning("App not installed");
            request.setAttribute("appDisabled", true);
        } else if (Arrays.stream(request.getParameter("modlist").split(","))
                .map(this::queryModById)
                .anyMatch(o -> o == null || o.getBoolean("_bIsWithheld") || o.getBoolean("_bIsTrashed") || o.getBoolean("_bIsPrivate"))) {

            logger.warning("Invalid mods");
            request.setAttribute("invalidMods", true);
        } else {
            // finally save!
            dbEntity.setProperty("title", request.getParameter("title"));
            dbEntity.setProperty("modList", request.getParameter("modlist"));
            datastore.put(dbEntity);
            request.setAttribute("saved", true);
            request.setAttribute("isInDatabase", true);
            logger.info("Save successful");
            if (newKey != null) {
                logger.info("Key newly generated");
                request.setAttribute("initialKey", newKey);
            }
        }

        request.getRequestDispatcher("/WEB-INF/gamebanana-modlist-settings.jsp").forward(request, response);
    }

    /**
     * Gets all users that installed the App. This excludes users that were banned.
     */
    private Set<String> getAllUsers() throws IOException {
        int page = 1;
        Set<String> result = new HashSet<>();
        while (true) {
            final Elements members = Jsoup.connect("https://gamebanana.com/apps/users/752?vl[page]=" + page + "&mid=UsersList").get()
                    .select("recordcell.Member a.Avatar");

            if (members.size() == 0) {
                // we reached the end of the pages!
                logger.info("User list: [" + String.join(", ", result) + "]");

                if (result.isEmpty()) {
                    // if we got no user, it's probably a bug (maybe the page changed and the selector does not work anymore)
                    // since I (max480) will be using the app regardless.
                    throw new RuntimeException("The user list is empty!");
                }

                return result;
            }

            for (Element member : members) {
                if (!member.select("img").attr("data-src").equals("https://images.gamebanana.com/img/av/banned.gif")) {
                    // this is a member and they're not banned!
                    result.add(member.attr("href").substring(member.attr("href").lastIndexOf("/") + 1));
                }
            }

            page++;
        }
    }

    /**
     * Deletes all settings of users that uninstalled the App.
     * Run daily at midnight, Paris time.
     */
    private void housekeep() throws IOException {
        // delete all non-users from the database
        Set<String> users = getAllUsers();
        final Query query = new Query("arbitraryModAppConfiguration").setKeysOnly();
        for (Entity entity : datastore.prepare(query).asIterable()) {
            if (!users.contains(entity.getKey().getName())) {
                logger.info("Deleting key " + entity.getKey().toString() + " from the database because they're not an app user.");
                datastore.delete(entity.getKey());
            }
        }
    }

}
