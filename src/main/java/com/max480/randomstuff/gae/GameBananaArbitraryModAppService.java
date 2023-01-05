package com.max480.randomstuff.gae;

import com.google.cloud.datastore.*;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.max480.randomstuff.gae.ConnectionUtils.openStreamWithTimeout;
import static java.nio.charset.StandardCharsets.UTF_8;

@WebServlet(name = "GameBananaArbitraryModAppService", urlPatterns = {"/gamebanana/arbitrary-mod-app",
        "/gamebanana/arbitrary-mod-app-settings", "/gamebanana/arbitrary-mod-app-housekeep", "/gamebanana/arbitrary-mod-app-modlist"})
public class GameBananaArbitraryModAppService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("GameBananaArbitraryModAppService");
    private static final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();
    private static final KeyFactory keyFactory = datastore.newKeyFactory().setKind("arbitraryModAppConfiguration");
    private final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

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
                logger.warning("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if (request.getRequestURI().equals("/gamebanana/arbitrary-mod-app-modlist")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                Set<String> modIds = getModList();
                response.setContentType("application/json");
                response.getWriter().write(new JSONArray(modIds).toString());
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

        Entity dbEntity = datastore.get(keyFactory.newKey(memberId));

        if (dbEntity == null) {
            // whoops, user doesn't exist yet.
            response.setHeader("Content-Type", "text/html");
            response.getWriter().write("<b>Arbitrary Mod App was not configured!</b>");
            return;
        }

        String name = dbEntity.getString("title");
        String list = dbEntity.getString("modList");

        List<ModInfo> modList = Arrays.stream(list.split(",")).parallel()
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

        request.setAttribute("isMax480", "1698143".equals(request.getParameter("_idProfile"));

        response.setHeader("Content-Type", "application/json");
        request.setAttribute("title", name);
        request.setAttribute("modList", modList);
        request.setAttribute("memberId", memberId);

        request.getRequestDispatcher("/WEB-INF/gamebanana-modlist.jsp").forward(request, response);
    }

    private JSONObject queryModById(String modId) {
        try {
            // try reading from the Cloud Storage cache, that is fed daily by the backend:
            // see https://github.com/max4805/RandomBackendStuff/blob/main/src/main/java/com/max480/randomstuff/backend/celeste/crontabs/ArbitraryModAppCacher.java
            BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", "arbitrary-mod-app-cache/" + modId + ".json");
            return new JSONObject(new String(storage.readAllBytes(blobId), UTF_8));
        } catch (Exception ex) {
            // if this is not possible, read from GameBanana directly instead
            logger.info("Could not retrieve mod by ID from cache, querying GameBanana directly: " + ex);
            try (InputStream is = openStreamWithTimeout("https://gamebanana.com/apiv8/Mod/" + modId +
                    "?_csvProperties=_sProfileUrl,_sName,_aPreviewMedia,_tsDateAdded,_tsDateUpdated,_aGame,_aRootCategory,_aSubmitter,_bIsWithheld,_bIsTrashed,_bIsPrivate,_nViewCount,_nLikeCount,_nPostCount")) {
                return new JSONObject(IOUtils.toString(is, UTF_8));
            } catch (IOException e) {
                logger.severe("Could not retrieve mod by ID! " + e);
                e.printStackTrace();
                return null;
            }
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

        Entity dbEntity = datastore.get(keyFactory.newKey(memberId));
        if (dbEntity == null) {
            // whoops, user doesn't exist yet.
            request.setAttribute("title", "");
            request.setAttribute("modList", "");
            request.setAttribute("isInDatabase", false);
        } else {
            request.setAttribute("title", dbEntity.getString("title"));
            request.setAttribute("modList", dbEntity.getString("modList"));
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

        Entity.Builder dbEntityBuilder;
        String key;

        Entity existingDbEntity = datastore.get(keyFactory.newKey(memberId));
        if (existingDbEntity == null) {
            // we must create the user with a random key.
            key = UUID.randomUUID().toString();
            dbEntityBuilder = Entity.newBuilder(keyFactory.newKey(memberId))
                    .set("key", key);
            request.setAttribute("isInDatabase", false);
        } else {
            key = existingDbEntity.getString("key");
            dbEntityBuilder = Entity.newBuilder(existingDbEntity);
            request.setAttribute("initialKey", "");
            request.setAttribute("isInDatabase", true);
        }

        // never trust the frontend.
        if (StringUtils.isEmpty(request.getParameter("title")) ||
                (existingDbEntity != null && StringUtils.isEmpty(request.getParameter("key"))) ||
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
        request.setAttribute("typedKey", existingDbEntity != null ? request.getParameter("key") : "");
        request.setAttribute("title", request.getParameter("title"));
        request.setAttribute("modList", request.getParameter("modlist"));

        if (existingDbEntity != null && !request.getParameter("key").equals(key)) {
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
            dbEntityBuilder
                    .set("title", request.getParameter("title"))
                    .set("modList", request.getParameter("modlist"));
            datastore.put(dbEntityBuilder.build());
            request.setAttribute("saved", true);
            request.setAttribute("isInDatabase", true);
            logger.info("Save successful");

            if (existingDbEntity == null) {
                logger.info("Key newly generated");
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
                userList = new JSONObject(IOUtils.toString(is, UTF_8));
            }

            JSONArray members = userList.getJSONArray("_aRecords");

            if (members.length() == 0) {
                // we reached the end of the pages!
                logger.info("User list: [" + String.join(", ", result) + "]");

                if (!result.contains("1698143")) {
                    // failsafe: max480 should be in the list, otherwise this means the listing does not work
                    throw new RuntimeException("The user list does not have max480 in it!");
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
        final QueryResults<Key> query = datastore.run(Query.newKeyQueryBuilder()
                .setKind("arbitraryModAppConfiguration")
                .build());

        List<Key> keysToDelete = new ArrayList<>();
        while (query.hasNext()) {
            Key key = query.next();
            if (!users.contains(key.getName())) {
                logger.info("Deleting key " + key + " from the database because they're not an app user.");
                keysToDelete.add(key);
            }
        }

        for (Key key : keysToDelete) {
            datastore.delete(key);
        }
    }

    private Set<String> getModList() {
        Set<String> result = new HashSet<>();

        final QueryResults<Entity> query = datastore.run(Query.newEntityQueryBuilder()
                .setKind("arbitraryModAppConfiguration")
                .build());

        while (query.hasNext()) {
            Entity entity = query.next();
            String list = entity.getString("modList");
            result.addAll(Arrays.asList(list.split(",")));
        }

        return result;
    }

}
