package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.max480.randomstuff.gae.GameBananaArbitraryModAppService.ArbitraryModAppSettings;
import static ovh.maddie480.randomstuff.frontend.ConnectionUtils.openStreamWithTimeout;

@WebServlet(name = "GameBananaArbitraryModAppService", loadOnStartup = 6, urlPatterns = {"/gamebanana/arbitrary-mod-app",
        "/gamebanana/arbitrary-mod-app-settings", "/gamebanana/arbitrary-mod-app-housekeep", "/gamebanana/arbitrary-mod-app-modlist"})
public class GameBananaArbitraryModAppService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(GameBananaArbitraryModAppService.class);

    private static Map<String, ArbitraryModAppSettings> database = new HashMap<>();

    @Override
    public void init() {
        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(Paths.get("/shared/gamebanana/arbitrary-mod-app-settings.ser")))) {
            database = (Map<String, ArbitraryModAppSettings>) is.readObject();
            log.debug("Loaded {} arbitrary mod app settings.", database.size());
        } catch (ClassNotFoundException | IOException e) {
            log.warn("Loading Arbitrary Mod App settings failed!", e);
        }
    }


    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("EE, MMM d yyyy h:mm a O", Locale.ENGLISH);

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
        public String categoryName; // _aRootCategory._sName
        public String categoryIcon; // _aRootCategory._sIconUrl
        public String categoryUrl; // _aRootCategory._sProfileUrl
        public String submitterAvatar; // _aSubmitter._sAvatarUrl
        public String submitterUrl; // _aSubmitter._sProfileUrl
        public String submitterName; // _aSubmitter._sName
        public String likeCount; // _nLikeCount
        public String viewCount; // _nViewCount
        public String postCount; // _nPostCount
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
                log.warn("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if (request.getRequestURI().equals("/gamebanana/arbitrary-mod-app-modlist")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                Set<String> modIds = getModList();
                response.setContentType("application/json");
                new JSONArray(modIds).write(response.getWriter());
            } else {
                // invalid secret
                log.warn("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        // check that the mandatory _idProfile parameter is present, respond 400 Bad Request otherwise.
        String memberId = request.getParameter("_idProfile");
        if (memberId == null) {
            log.warn("Bad Request");
            response.setStatus(400);
            return;
        }

        ArbitraryModAppSettings dbEntity = database.get(memberId);

        if (dbEntity == null) {
            // whoops, user doesn't exist yet.
            response.setHeader("Content-Type", "text/html");
            response.getWriter().write("<b>Arbitrary Mod App was not configured!</b>");
            return;
        }

        String name = dbEntity.title;
        List<String> list = dbEntity.modList;

        List<ModInfo> modList = list.stream().parallel()
                // request all mods the user asked for.
                .map(this::queryModById)
                // take out the ones that errored or were withheld / trashed.
                .filter(o -> o != null && !o.getBoolean("_bIsWithheld") && !o.getBoolean("_bIsTrashed") && !o.getBoolean("_bIsPrivate"))
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

                    info.categoryName = object.getJSONObject("_aRootCategory").getString("_sName");
                    info.categoryIcon = object.getJSONObject("_aRootCategory").getString("_sIconUrl");
                    info.categoryUrl = object.getJSONObject("_aRootCategory").getString("_sProfileUrl");

                    info.submitterName = object.getJSONObject("_aSubmitter").getString("_sName");
                    info.submitterAvatar = object.getJSONObject("_aSubmitter").getString("_sAvatarUrl");
                    info.submitterUrl = object.getJSONObject("_aSubmitter").getString("_sProfileUrl");

                    info.likeCount = bigNumberToString(object.getInt("_nLikeCount"));
                    info.viewCount = bigNumberToString(object.getInt("_nViewCount"));
                    info.postCount = bigNumberToString(object.getInt("_nPostCount"));

                    return info;
                })
                .collect(Collectors.toList());

        request.setAttribute("isMaddie", "1698143".equals(request.getParameter("_idProfile")));

        response.setHeader("Content-Type", "application/json");
        request.setAttribute("title", name);
        request.setAttribute("modList", modList);
        request.setAttribute("memberId", memberId);

        request.getRequestDispatcher("/WEB-INF/gamebanana-modlist.jsp").forward(request, response);
    }

    private JSONObject queryModById(String modId) {
        try {
            Path cache = Paths.get("/shared/temp/arbitrary-mod-app-cache/" + modId + ".json");
            if (Files.exists(cache)) {
                // try reading from the cache, that is fed daily by the backend:
                // see https://github.com/maddie480/RandomBackendStuff/blob/main/src/main/java/ovh/maddie480/randomstuff/backend/celeste/crontabs/ArbitraryModAppCacher.java
                return new JSONObject(Files.readString(cache));
            } else {
                // if this is not possible, read from GameBanana directly instead
                log.info("Could not retrieve mod by ID from cache, querying GameBanana directly");
                try (InputStream is = openStreamWithTimeout("https://gamebanana.com/apiv8/Mod/" + modId +
                        "?_csvProperties=_sProfileUrl,_sName,_aPreviewMedia,_tsDateAdded,_tsDateUpdated,_aGame,_aRootCategory,_aSubmitter,_bIsWithheld,_bIsTrashed,_bIsPrivate,_nViewCount,_nLikeCount,_nPostCount")) {
                    return new JSONObject(new JSONTokener(is));
                }
            }
        } catch (IOException e) {
            log.error("Could not retrieve mod by ID! ", e);
            e.printStackTrace();
            return null;
        }
    }

    private void getSettingsPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // check that the mandatory _idMember parameter is present, respond 400 Bad Request otherwise.
        String memberId = request.getParameter("_idMember");
        if (memberId == null) {
            log.warn("Bad Request");
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

        ArbitraryModAppSettings dbEntity = database.get(memberId);
        if (dbEntity == null) {
            // whoops, user doesn't exist yet.
            request.setAttribute("title", "");
            request.setAttribute("modList", "");
            request.setAttribute("isInDatabase", false);
        } else {
            request.setAttribute("title", dbEntity.title);
            request.setAttribute("modList", String.join(",", dbEntity.modList));
            request.setAttribute("isInDatabase", true);
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

    private String bigNumberToString(int number) {
        if (number < 1000) {
            return Integer.toString(number);
        } else if (number < 1_000_000) {
            return new DecimalFormat("0.#").format(number / 1000.) + "k";
        } else {
            return new DecimalFormat("0.#").format(number / 1_000_000.) + "m";
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getRequestURI().equals("/gamebanana/arbitrary-mod-app-settings")) {
            // Method Not Allowed: /gamebanana/arbitrary-mod-app only supports GET
            log.warn("Method Not Allowed");
            response.setStatus(405);
            return;
        }

        // check that the mandatory _idMember parameter is present, respond 400 Bad Request otherwise.
        String memberId = request.getParameter("_idMember");
        if (memberId == null) {
            log.warn("Bad Request");
            response.setStatus(400);
            return;
        }

        String key;

        ArbitraryModAppSettings dbEntity = database.get(memberId);
        boolean entityIsInDatabase;

        if (dbEntity == null) {
            // we must create the user with a random key.
            key = UUID.randomUUID().toString();
            dbEntity = new ArbitraryModAppSettings();
            dbEntity.key = key;
            entityIsInDatabase = false;
        } else {
            key = dbEntity.key;
            request.setAttribute("initialKey", "");
            entityIsInDatabase = true;
        }

        request.setAttribute("isInDatabase", entityIsInDatabase);

        // never trust the frontend.
        if (StringUtils.isEmpty(request.getParameter("title")) ||
                (entityIsInDatabase && StringUtils.isEmpty(request.getParameter("key"))) ||
                request.getParameter("modlist") == null ||
                !request.getParameter("modlist").matches("([0-9]+,)*[0-9]+")) {

            // the user must have bypassed the HTML5 validations, so just send them an answer just like if they did a GET request.
            log.warn("POST was given up due to invalid parameters.");
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
        request.setAttribute("typedKey", entityIsInDatabase ? request.getParameter("key") : "");
        request.setAttribute("title", request.getParameter("title"));
        request.setAttribute("modList", request.getParameter("modlist"));

        if (entityIsInDatabase && !request.getParameter("key").equals(key)) {
            log.warn("Invalid key");
            request.setAttribute("invalidKey", true);
        } else if (request.getParameter("modlist").split(",").length > 50) {
            log.warn("Too many mods");
            request.setAttribute("tooManyMods", true);
        } else if (!getAllUsers().contains(memberId)) {
            log.warn("App not installed");
            request.setAttribute("appDisabled", true);
        } else if (Arrays.stream(request.getParameter("modlist").split(","))
                .map(this::queryModById)
                .anyMatch(o -> o == null || o.getBoolean("_bIsWithheld") || o.getBoolean("_bIsTrashed") || o.getBoolean("_bIsPrivate"))) {

            log.warn("Invalid mods");
            request.setAttribute("invalidMods", true);
        } else {
            // finally save!
            dbEntity.title = request.getParameter("title");
            dbEntity.modList = Arrays.asList(request.getParameter("modlist").split(","));
            database.put(memberId, dbEntity);
            saveDatabase();

            request.setAttribute("saved", true);
            request.setAttribute("isInDatabase", true);
            log.info("Save successful");

            if (!entityIsInDatabase) {
                log.info("Key newly generated");
                request.setAttribute("initialKey", key);
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
            JSONObject userList;
            try (InputStream is = ConnectionUtils.openStreamWithTimeout("https://gamebanana.com/apiv10/App/752/Users?_nPerpage=50&_nPage=" + page)) {
                userList = new JSONObject(new JSONTokener(is));
            }

            JSONArray members = userList.getJSONArray("_aRecords");

            if (members.length() == 0) {
                // we reached the end of the pages!
                log.info("User list: [" + String.join(", ", result) + "]");

                if (!result.contains("1698143")) {
                    // failsafe: Maddie should be in the list, otherwise this means the listing does not work
                    throw new RuntimeException("The user list does not have Maddie in it!");
                }

                return result;
            }

            for (Object member : members) {
                result.add(Integer.toString(((JSONObject) member).getJSONObject("_aUser").getInt("_idRow")));
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

        List<String> keysToDelete = new ArrayList<>();
        for (String userId : database.keySet()) {
            if (!users.contains(userId)) {
                log.info("Deleting key {} from the database because they're not an app user.", userId);
                keysToDelete.add(userId);
            }
        }

        for (String key : keysToDelete) {
            database.remove(key);
        }
        saveDatabase();
    }

    private Set<String> getModList() {
        Set<String> result = new HashSet<>();

        for (ArbitraryModAppSettings settings : database.values()) {
            result.addAll(settings.modList);
        }

        return result;
    }

    private void saveDatabase() throws IOException {
        try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(Paths.get("/shared/gamebanana/arbitrary-mod-app-settings.ser")))) {
            os.writeObject(database);
        }
    }
}
