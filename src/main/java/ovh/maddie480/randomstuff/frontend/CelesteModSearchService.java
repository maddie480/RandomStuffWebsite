package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.max480.randomstuff.backend.celeste.crontabs.UpdateCheckerTracker.ModInfo;

/**
 * This servlet provides the GameBanana search API, and other APIs that are used by Olympus or the Banana Mirror Browser.
 */
@WebServlet(name = "CelesteModSearchService", loadOnStartup = 2, urlPatterns = {"/celeste/gamebanana-search",
        "/celeste/gamebanana-search-reload", "/celeste/gamebanana-list", "/celeste/gamebanana-categories", "/celeste/gamebanana-info",
        "/celeste/random-map", "/celeste/gamebanana-featured", "/celeste/everest-versions", "/celeste/everest-versions-reload",
        "/celeste/olympus-versions", "/celeste/loenn-versions"})
public class CelesteModSearchService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteModSearchService.class);

    private static List<ModInfo> modDatabaseForSorting = Collections.emptyList();
    private Map<Integer, String> modCategories;

    private byte[] everestVersionsNoNative;
    private byte[] everestVersionsWithNative;

    private static final Pattern SUPPORT_NATIVE_REGEX = Pattern.compile("(^|&)supportsNativeBuilds=true($|&)");

    private static final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void init() {
        try {
            refreshModDatabase();
            refreshEverestVersions();
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/gamebanana-search-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshModDatabase();
            } else {
                // invalid secret
                log.warn("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if (request.getRequestURI().equals("/celeste/random-map")) {
            List<ModInfo> maps = modDatabaseForSorting.stream()
                    .filter(i -> "Mod".equals(i.type) && i.categoryId == 6800) // Map
                    .toList();

            // pick a map and redirect to it. that's it.
            ModInfo drawnMod = maps.get(secureRandom.nextInt(maps.size()));
            response.setStatus(302);
            response.setHeader("Location", "https://gamebanana.com/mods/" + drawnMod.id);
            return;
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-search")) {
            String queryParam = request.getParameter("q");

            if (queryParam == null || queryParam.trim().isEmpty()) {
                // the user didn't give any search!
                response.setHeader("Content-Type", "text/plain");
                log.warn("Bad request");
                response.setStatus(400);
                response.getWriter().write("\"q\" query parameter expected");
            } else {
                List<Map<String, Object>> responseBody = searchModsByName(queryParam);
                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(new JSONArray(responseBody).toString());
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-list")) {
            String sortParam = request.getParameter("sort");
            String pageParam = request.getParameter("page");
            String typeParam = request.getParameter("type");
            String categoryParam = request.getParameter("category");

            if (!Arrays.asList("latest", "likes", "views", "downloads").contains(sortParam)) {
                // invalid sort!
                response.setHeader("Content-Type", "text/plain");
                log.warn("Bad request");
                response.setStatus(400);
                response.getWriter().write("expected \"sort\" parameter with value \"latest\", \"likes\", \"views\" or \"downloads\"");
            } else {
                // parse the page number: if page number is absent or invalid, assume 1
                int page = 1;
                if (pageParam != null) {
                    try {
                        page = Integer.parseInt(pageParam);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid page number, assuming 1");
                    }
                }

                // is there a type and/or a category filter?
                Predicate<ModInfo> typeFilter = info -> true;
                if (typeParam != null) {
                    if (categoryParam != null) {
                        typeFilter = info -> typeParam.equalsIgnoreCase(info.type) && Integer.toString(info.categoryId).equals(categoryParam);
                    } else {
                        typeFilter = info -> typeParam.equalsIgnoreCase(info.type);
                    }
                } else if (categoryParam != null) {
                    typeFilter = info -> Integer.toString(info.categoryId).equals(categoryParam);
                }

                // determine the field on which we want to sort. Sort by descending id to tell equal values apart.
                Comparator<ModInfo> sort = switch (sortParam) {
                    case "views" -> Comparator.<ModInfo>comparingInt(i -> -i.views).thenComparingInt(i -> -i.id);
                    case "likes" -> Comparator.<ModInfo>comparingInt(i -> -i.likes).thenComparingInt(i -> -i.id);
                    case "downloads" ->
                            Comparator.<ModInfo>comparingInt(i -> -i.downloads).thenComparingInt(i -> -i.id);
                    case "latest" -> Comparator.<ModInfo>comparingInt(i -> -i.createdDate).thenComparingInt(i -> -i.id);
                    default -> null;
                };

                // then sort on it.
                Stream<ModInfo> responseBodyStream = modDatabaseForSorting.stream()
                        .filter(typeFilter);

                if (sort != null) {
                    responseBodyStream = responseBodyStream.sorted(sort);
                }

                final List<Map<String, Object>> responseBody = responseBodyStream
                        .skip((page - 1) * 20L)
                        .limit(20)
                        .map(modInfo -> modInfo.fullInfo)
                        .collect(Collectors.toList());

                // count the amount of results and put it as a header.
                response.setHeader("X-Total-Count", Long.toString(modDatabaseForSorting.stream()
                        .filter(typeFilter)
                        .count()));

                response.setHeader("Content-Type", "application/json");
                response.getWriter().write(new JSONArray(responseBody).toString());
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-info")) {
            String itemtype = request.getParameter("itemtype");

            Integer itemid = null;
            try {
                if (request.getParameter("itemid") != null) {
                    itemid = Integer.parseInt(request.getParameter("itemid"));
                }
            } catch (NumberFormatException e) {
                log.warn("Cannot parse itemid as number", e);
            }

            if (itemtype == null || itemid == null) {
                // missing parameter
                log.warn("Bad request");
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(400);
                response.getWriter().write("'itemtype' and 'itemid' query params should both be specified, and itemid should be a valid number");
            } else {
                final int itemId = itemid;
                String responseBody = modDatabaseForSorting.stream()
                        .filter(mod -> itemtype.equals(mod.type) && itemId == mod.id)
                        .findFirst()
                        .map(mod -> new JSONObject(mod.fullInfo).toString())
                        .orElse(null);

                // send out the response.
                if (responseBody != null) {
                    response.setHeader("Content-Type", "application/json");
                    response.getWriter().write(responseBody);
                } else {
                    log.warn("Not found");
                    response.setHeader("Content-Type", "text/plain");
                    response.setStatus(404);
                    response.getWriter().write("Not Found");
                }
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-featured")) {
            final List<String> catOrder = Arrays.asList("today", "week", "month", "3month", "6month", "year", "alltime");
            final List<Map<String, Object>> responseBody = modDatabaseForSorting.stream()
                    .filter(mod -> mod.fullInfo.containsKey("Featured"))
                    .sorted((a, b) -> {
                        Map<String, Object> aInfo = (Map<String, Object>) a.fullInfo.get("Featured");
                        Map<String, Object> bInfo = (Map<String, Object>) b.fullInfo.get("Featured");

                        // sort by category, then by position.
                        if (aInfo.get("Category").equals(bInfo.get("Category"))) {
                            return (int) aInfo.get("Position") - (int) bInfo.get("Position");
                        }
                        return catOrder.indexOf(aInfo.get("Category")) - catOrder.indexOf(bInfo.get("Category"));
                    })
                    .map(mod -> mod.fullInfo)
                    .collect(Collectors.toList());

            response.setHeader("Content-Type", "application/json");
            response.getWriter().write(new JSONArray(responseBody).toString());
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-categories")) {
            // go across all mods and aggregate stats per category.
            HashMap<Object, Integer> categoriesAndCounts = new HashMap<>();
            for (ModInfo modInfo : modDatabaseForSorting) {
                Object category = modInfo.type;
                if (category.equals("Mod")) {
                    category = modInfo.categoryId;
                }
                if (!categoriesAndCounts.containsKey(category)) {
                    // first mod encountered in this category
                    categoriesAndCounts.put(category, 1);
                } else {
                    // add 1 to the mod count in the category
                    categoriesAndCounts.put(category, categoriesAndCounts.get(category) + 1);
                }
            }

            // format the map for the response...
            List<Map<String, Object>> categoriesList = categoriesAndCounts.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        if (entry.getKey() instanceof String) {
                            // itemtype
                            result.put("itemtype", entry.getKey());
                            result.put("formatted", formatGameBananaItemtype(entry.getKey().toString(), true));
                        } else {
                            // mod category
                            result.put("itemtype", "Mod");
                            result.put("categoryid", entry.getKey());
                            result.put("formatted", modCategories.get(entry.getKey()));
                        }
                        result.put("count", entry.getValue());
                        return result;
                    })
                    .sorted(Comparator.comparing(result -> result.get("formatted").toString()))
                    .toList();

            // also add an "All" option to pass the total number of mods.
            Map<String, Object> all = new HashMap<>();
            all.put("formatted", "All");
            all.put("count", modDatabaseForSorting.size());

            // the final list is "All" followed by all the categories.
            List<Map<String, Object>> responseBody = new ArrayList<>();
            responseBody.add(all);
            responseBody.addAll(categoriesList);

            // send out the response (the "block" flow style works better with Olympus).
            response.setHeader("Content-Type", "text/yaml");
            YamlUtil.dump(responseBody, response.getOutputStream());
        }

        if (request.getRequestURI().equals("/celeste/everest-versions-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshEverestVersions();
            } else {
                // invalid secret
                log.warn("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        if ("/celeste/everest-versions".equals(request.getRequestURI())) {
            // send everest_version_list.json we downloaded earlier
            response.setHeader("Content-Type", "application/json");

            if (request.getQueryString() != null && SUPPORT_NATIVE_REGEX.matcher(request.getQueryString()).matches()) {
                IOUtils.write(everestVersionsWithNative, response.getOutputStream());
            } else {
                IOUtils.write(everestVersionsNoNative, response.getOutputStream());
            }
        }

        if ("/celeste/olympus-versions".equals(request.getRequestURI())) {
            // send olympus-versions.json
            response.setHeader("Content-Type", "application/json");

            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/olympus-versions.json"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        }

        if ("/celeste/loenn-versions".equals(request.getRequestURI())) {
            // send loenn-versions.json
            response.setHeader("Content-Type", "application/json");

            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/loenn-versions.json"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        }
    }

    public static List<Map<String, Object>> searchModsByName(String queryParam) {
        final String[] tokenizedRequest = tokenize(queryParam);

        return modDatabaseForSorting.stream()
                .filter(mod -> scoreMod(tokenizedRequest, (String[]) mod.fullInfo.get("TokenizedName")) > 0.2f * tokenizedRequest.length)
                .sorted(Comparator
                        .<ModInfo>comparingDouble(mod -> -scoreMod(tokenizedRequest, (String[]) mod.fullInfo.get("TokenizedName")))
                        .thenComparingInt(mod -> -mod.downloads))
                .map(mod -> mod.fullInfo)
                .limit(20)
                .collect(Collectors.toList());
    }

    private static String[] tokenize(String string) {
        string = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9* ]", "");
        while (string.contains("  ")) string = string.replace("  ", " ");
        return string.split(" ");
    }

    private static double scoreMod(String[] query, String[] modName) {
        double score = 0;

        for (String tokenSearch : query) {
            if (tokenSearch.endsWith("*")) {
                // "starts with" search: add 1 if there's a word starting with the prefix
                String tokenSearchStart = tokenSearch.substring(0, tokenSearch.length() - 1);
                for (String tokenModName : modName) {
                    if (tokenModName.startsWith(tokenSearchStart)) {
                        score++;
                        break;
                    }
                }
            } else {
                // "equals" search: take the score of the word that is closest to the token
                double tokenScore = 0;
                for (String tokenModName : modName) {
                    tokenScore = Math.max(tokenScore, Math.pow(0.5, LevenshteinDistance.getDefaultInstance().apply(tokenSearch, tokenModName)));
                }
                score += tokenScore;
            }
        }

        return score;
    }

    public static String formatGameBananaItemtype(String input, boolean pluralize) {
        // specific formatting for a few categories
        switch (input) {
            case "Gamefile" -> {
                return pluralize ? "Game files" : "Game file";
            }
            case "Wip" -> {
                return pluralize ? "WiPs" : "WiP";
            }
            case "Gui" -> {
                return pluralize ? "GUIs" : "GUI";
            }
        }

        // apply the spaced pascal case from Everest
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(input.charAt(i - 1)))
                builder.append(' ');

            if (i != 0 && builder.charAt(builder.length() - 1) == ' ') {
                builder.append(Character.toUpperCase(c));
            } else {
                builder.append(c);
            }
        }

        // capitalize the first letter
        String result = builder.toString();
        result = result.substring(0, 1).toUpperCase() + result.substring(1);

        if (!pluralize) {
            return result;
        }

        // pluralize
        if (result.charAt(result.length() - 1) == 'y') {
            return result.substring(0, result.length() - 1) + "ies";
        }
        return result + "s";
    }

    // mapping takes an awful amount of time on App Engine (~2 seconds) so we can't make it when the user calls the API.
    private void refreshModDatabase() throws IOException {
        // get and deserialize the mod list from storage.
        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(Paths.get("/shared/celeste/mod-search-database.ser")))) {
            modDatabaseForSorting = (List<ModInfo>) is.readObject();
            modCategories = (Map<Integer, String>) is.readObject();
            log.debug("There are " + modDatabaseForSorting.size() + " mods in the search database.");
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    private void refreshEverestVersions() throws IOException {
        everestVersionsNoNative = IOUtils.toByteArray(Files.newInputStream(Paths.get("/shared/celeste/everest-versions.json")));
        everestVersionsWithNative = IOUtils.toByteArray(Files.newInputStream(Paths.get("/shared/celeste/everest-versions-with-native.json")));
        log.debug("Reloaded Everest versions! Preloaded {} bytes with native versions, {} bytes without native versions.",
                everestVersionsWithNative.length, everestVersionsNoNative.length);
    }

    public static ModInfo getModInfoByTypeAndId(String itemtype, int itemid) {
        return modDatabaseForSorting.stream()
                .filter(m -> m.type.equals(itemtype) && m.id == itemid)
                .findFirst()
                .orElse(null);
    }
}
