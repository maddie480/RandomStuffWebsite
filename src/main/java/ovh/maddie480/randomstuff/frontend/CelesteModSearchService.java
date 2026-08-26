package ovh.maddie480.randomstuff.frontend;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.ModDatabase;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.model.CategoryRecord;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.model.ModRecord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This servlet provides the GameBanana search API, and other APIs that are used by Olympus or the Banana Mirror Browser.
 */
@WebServlet(name = "CelesteModSearchService", loadOnStartup = 1, urlPatterns = {"/celeste/gamebanana-search",
        "/celeste/gamebanana-search-reload", "/celeste/gamebanana-list", "/celeste/gamebanana-categories", "/celeste/gamebanana-info",
        "/celeste/random-map", "/celeste/gamebanana-featured", "/celeste/everest-versions", "/celeste/everest-versions-reload",
        "/celeste/olympus-versions", "/celeste/loenn-versions", "/celeste/helper-list", "/celeste/gamebanana-subcategories",
        "/celeste/mod_ids_to_names.json", "/celeste/mod_ids_to_categories.json"})
public class CelesteModSearchService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteModSearchService.class);

    public static List<ModRecord> database = Collections.emptyList();

    private byte[] everestVersions;
    private byte[] helperList;
    private byte[] modIdsToNames;
    private byte[] modIdsToCategories;
    private byte[] precomputedCategoryList;
    private byte[] precomputedSubcategoryList;

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
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/celeste/gamebanana-search-reload")) {
            handleSearchReload(request, response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/random-map")) {
            handleRandomMap(response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/gamebanana-search")) {
            handleModSearch(request, response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/gamebanana-list")) {
            handleModList(request, response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/gamebanana-info")) {
            handleSingleModInfo(request, response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/gamebanana-featured")) {
            handleFeaturedModsList(response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/gamebanana-categories")) {
            handleCategoriesList(response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/gamebanana-subcategories")) {
            handleSubcategoriesList(response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/helper-list")) {
            handleHelperList(response);
            return;
        }
        if (request.getRequestURI().equals("/celeste/everest-versions-reload")) {
            handleEverestVersionsReload(request, response);
            return;
        }
        if ("/celeste/everest-versions".equals(request.getRequestURI())) {
            handleEverestVersionsList(response);
            return;
        }
        if ("/celeste/olympus-versions".equals(request.getRequestURI())) {
            handleOlympusAndLoennVersionsList(response, "/shared/celeste/olympus-versions.json");
            return;
        }
        if ("/celeste/loenn-versions".equals(request.getRequestURI())) {
            handleOlympusAndLoennVersionsList(response, "/shared/celeste/loenn-versions.json");
            return;
        }
        if ("/celeste/mod_ids_to_names.json".equals(request.getRequestURI())) {
            handleModIdsToNamesList(response);
            return;
        }
        if ("/celeste/mod_ids_to_categories.json".equals(request.getRequestURI())) {
            handleModIdsToCategoriesList(response);
            return;
        }
    }

    private void handleSearchReload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
            refreshModDatabase();
        } else {
            // invalid secret
            log.warn("Invalid key");
            response.setStatus(403);
        }
    }

    private static void handleRandomMap(HttpServletResponse response) {
        List<ModRecord> maps = database.stream()
                .filter(m -> {
                    CategoryRecord cat = m.category;
                    while (cat != null) {
                        if (cat.name.equals("Maps")) return true;
                        cat = cat.parent;
                    }
                    return false;
                })
                .toList();

        // pick a map and redirect to it. that's it.
        ModRecord drawnMod = maps.get(secureRandom.nextInt(maps.size()));
        response.setStatus(302);
        response.setHeader("Location", drawnMod.pageUrl);
    }

    private static void handleModSearch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String queryParam = request.getParameter("q");

        if (queryParam == null || queryParam.trim().isEmpty()) {
            // the user didn't give any search!
            response.setHeader("Content-Type", "text/plain");
            log.warn("Bad request for mod search");
            response.setStatus(400);
            response.getWriter().write("\"q\" query parameter expected");
        } else {
            List<Map<String, Object>> responseBody = searchModsByName(queryParam);
            response.setHeader("Content-Type", "application/json");
            new JSONArray(responseBody).write(response.getWriter());
        }
    }

    private static void handleModList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sortParam = request.getParameter("sort");
        String pageParam = request.getParameter("page");
        String categoryParam = request.getParameter("category");
        String subcategoryParam = request.getParameter("subcategory");

        if (!Arrays.asList("latest", "likes", "views", "downloads").contains(sortParam)) {
            // invalid sort!
            response.setHeader("Content-Type", "text/plain");
            log.warn("Bad request for mod list");
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
            List<Predicate<ModRecord>> typeFilters = new ArrayList<>();
            if (categoryParam != null) {
                typeFilters.add(info -> {
                    CategoryRecord c = info.category;
                    while (c.parent != null) c = c.parent;
                    return c.id.equals(categoryParam);
                });
            }
            if (subcategoryParam != null) {
                typeFilters.add(info -> info.category.id.equals(subcategoryParam));
            }
            // typeFilter is a && of all typeFilters
            Predicate<ModRecord> typeFilter = info -> typeFilters.stream().allMatch(filter -> filter.test(info));

            // determine the field on which we want to sort. Sort by descending id to tell equal values apart.
            Comparator<ModRecord> sort = switch (sortParam) {
                case "views" -> Comparator.<ModRecord>comparingInt(i -> -i.views).thenComparing(i -> i.id);
                case "likes" -> Comparator.<ModRecord>comparingInt(i -> -i.likes).thenComparing(i -> i.id);
                case "downloads" -> Comparator.<ModRecord>comparingInt(i -> -i.downloads).thenComparing(i -> i.id);
                case "latest" -> Comparator.<ModRecord>comparingLong(i -> -i.createdDate).thenComparing(i -> i.id);
                default -> null;
            };

            // then sort on it.
            Stream<ModRecord> responseBodyStream = database.stream()
                    .filter(typeFilter);

            if (sort != null) {
                responseBodyStream = responseBodyStream.sorted(sort);
            }

            final List<Map<String, Object>> responseBody = responseBodyStream
                    .skip((page - 1) * 20L)
                    .limit(20)
                    .map(CelesteModSearchService::serializeModInfo)
                    .map(CelesteModSearchService::crabify)
                    .collect(Collectors.toList());

            // count the amount of results and put it as a header.
            response.setHeader("X-Total-Count", Long.toString(database.stream()
                    .filter(typeFilter)
                    .count()));

            response.setHeader("Content-Type", "application/json");
            new JSONArray(responseBody).write(response.getWriter());
        }
    }

    private static void handleSingleModInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");

        if (id == null) {
            // missing parameter
            log.warn("Bad request");
            response.setHeader("Content-Type", "text/plain");
            response.setStatus(400);
            response.getWriter().write("'id' query param should be specified");
        } else {
            JSONObject responseBody = database.stream()
                    .filter(mod -> mod.id.equals(id))
                    .findFirst()
                    .map(mod -> new JSONObject(serializeModInfo(mod)))
                    .orElse(null);

            // send out the response.
            if (responseBody != null) {
                response.setHeader("Content-Type", "application/json");
                responseBody.write(response.getWriter());
            } else {
                log.warn("Not found");
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(404);
                response.getWriter().write("Not Found");
            }
        }
    }

    private static void handleFeaturedModsList(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        // oops
        new JSONArray().write(response.getWriter());
    }

    private void handleCategoriesList(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/yaml");
        response.getOutputStream().write(precomputedCategoryList);
    }

    private List<Map<String, Object>> computeCategoryList() {
        HashMap<CategoryRecord, Integer> categoriesAndCounts = new HashMap<>();
        for (ModRecord modInfo : database) {
            CategoryRecord category = modInfo.category;
            while (category.parent != null) category = category.parent;
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
                    result.put("itemtype", "Obsolete");
                    result.put("categoryid", entry.getKey().id);
                    result.put("formatted", entry.getKey().name);
                    result.put("count", entry.getValue());
                    return result;
                })
                .sorted(Comparator.comparing(result -> result.get("formatted").toString()))
                .toList();

        // also add an "All" option to pass the total number of mods.
        Map<String, Object> all = new HashMap<>();
        all.put("formatted", "All");
        all.put("count", database.size());

        // the final list is "All" followed by all the categories.
        List<Map<String, Object>> responseBody = new ArrayList<>();
        responseBody.add(all);
        responseBody.addAll(categoriesList);
        return responseBody;
    }

    private void handleSubcategoriesList(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/yaml");
        response.getOutputStream().write(precomputedSubcategoryList);
    }

    private List<Map<String, Object>> computeSubcategoryListFor(String categoryId) {
        Map<CategoryRecord, Integer> groupResult = database.stream()
                .filter(mod -> {
                    CategoryRecord category = mod.category;
                    while (category.parent != null) category = category.parent;
                    return category.id.equals(categoryId);
                })
                .collect(Collectors.toMap(
                        m -> categoryId.equals(m.category.id) ? new CategoryRecord() : m.category,
                        _ -> 1,
                        Integer::sum
                ));

        int total = groupResult.values().stream().mapToInt(i -> i).sum();

        // format the map for the response...
        List<Map<String, Object>> subcategoriesList = groupResult.entrySet().stream()
                .filter(cat -> cat.getKey().id != null)
                .<Map<String, Object>>map(entry -> ImmutableMap.of(
                        "id", entry.getKey().id,
                        "name", entry.getKey().name,
                        "count", entry.getValue()
                ))
                .sorted(Comparator.comparing(map -> map.get("name").toString()))
                .toList();

        // the final list is "All" followed by all the categories.
        List<Map<String, Object>> responseBody = new ArrayList<>();
        responseBody.add(ImmutableMap.of(
                "name", "All",
                "count", total
        ));
        responseBody.addAll(subcategoriesList);
        return responseBody;
    }

    private void handleHelperList(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.getOutputStream().write(helperList);
    }

    private void handleEverestVersionsReload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
            refreshEverestVersions();
        } else {
            // invalid secret
            log.warn("Invalid key for Everest version reload");
            response.setStatus(403);
        }
    }

    private void handleEverestVersionsList(HttpServletResponse response) throws IOException {
        // send everest-versions.json we loaded earlier
        response.setHeader("Content-Type", "application/json");
        IOUtils.write(everestVersions, response.getOutputStream());
    }

    private static List<Map<String, Object>> searchModsByName(String queryParam) {
        return searchModsByNameForInternal(queryParam).stream()
                .map(CelesteModSearchService::serializeModInfo)
                .limit(20)
                .map(CelesteModSearchService::crabify)
                .collect(Collectors.toList());
    }

    public static List<ModRecord> searchModsByNameForInternal(String queryParam) {
        final String[] tokenizedRequest = tokenize(queryParam);

        Map<ModRecord, Double> scoredMods = database.stream()
                .collect(Collectors.toMap(m -> m, m -> scoreMod(tokenizedRequest, tokenize(m.name))));

        return database.stream()
                .filter(mod -> scoredMods.get(mod) > 0.2f * tokenizedRequest.length)
                .sorted(Comparator
                        .<ModRecord>comparingDouble(mod -> -scoredMods.get(mod))
                        .thenComparingInt(mod -> -mod.downloads))
                .limit(20)
                .collect(Collectors.toList());
    }

    private static void handleOlympusAndLoennVersionsList(HttpServletResponse response, String first) throws IOException {
        response.setHeader("Content-Type", "application/json");

        try (InputStream is = Files.newInputStream(Paths.get(first))) {
            IOUtils.copy(is, response.getOutputStream());
        }
    }

    private void handleModIdsToNamesList(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.getOutputStream().write(modIdsToNames);
    }

    private void handleModIdsToCategoriesList(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.getOutputStream().write(modIdsToCategories);
    }

    private static String[] tokenize(String string) {
        string = StringUtils.stripAccents(string.toLowerCase(Locale.ROOT)) // "Pokémon" => "pokemon"
                .replace("'", "") // "Maddie's Helping Hand" => "maddies helping hand"
                .replaceAll("[^a-z0-9* ]", " "); // "The D-Sides Pack" => "the d sides pack"
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

    // context for the April Fools crab jokes:
    // https://www.reddit.com/r/celestegame/comments/128kg44/psa_for_modded_players_do_not_press_the_crab/
    public static double getCrabLevel() {
        ZonedDateTime now = ZonedDateTime.now();
        double crabLevel = 0;
        if (now.getMonthValue() == 3 && now.getDayOfMonth() == 31 && now.getHour() >= 12 && now.getHour() < 18)
            crabLevel = 0.1;
        if (now.getMonthValue() == 3 && now.getDayOfMonth() == 31 && now.getHour() >= 18) crabLevel = 0.5;
        if (now.getMonthValue() == 4 && now.getDayOfMonth() == 1) crabLevel = 1;
        if (now.getMonthValue() == 4 && now.getDayOfMonth() == 2 && now.getHour() < 6) crabLevel = 0.5;
        if (now.getMonthValue() == 4 && now.getDayOfMonth() == 2 && now.getHour() >= 6 && now.getHour() < 12)
            crabLevel = 0.1;
        if (crabLevel > 0) log.debug("April Fools crab level is {}", crabLevel);
        return crabLevel;
    }

    private static Map<String, Object> crabify(Map<String, Object> input) {
        if (Math.random() >= getCrabLevel()) return input;
        Map<String, Object> output = new HashMap<>(input);
        output.put("MirroredScreenshots", Arrays.asList("https://maddie480.ovh/img/crabulous_april_fools.png", "https://maddie480.ovh/img/crabulous_april_fools.png"));
        return output;
    }

    private void refreshModDatabase() throws IOException {
        // free up some memory by temporarily breaking the file searcher...
        database.forEach(m -> Arrays.stream(m.files)
                .forEach(f -> f.fileListing = new String[0]));

        // get and deserialize the mod list from storage.
        database = new ModDatabase().allMods;

        refreshCategoriesLists();
        refreshHelperList();
        refreshModIDsToNamesMap();
    }

    private void refreshHelperList() {
        Set<String> helpers = database.stream()
                .filter(mod -> {
                    CategoryRecord c = mod.category;
                    while (c.parent != null) c = c.parent;
                    return "Helpers".equals(c.name);
                })
                .map(m -> Arrays.stream(m.files)
                        .filter(f -> f.isLeader)
                        .map(f -> f.modId)
                        .toList())
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        // add a few manually
        helpers.add("AurorasHelper"); // categorized as Tool
        helpers.add("BGswitch"); // Other/Misc
        helpers.add("ColoredLights"); // Other/Misc
        helpers.add("corkr900GraphicsPack"); // Asset
        helpers.add("DisposableTheo"); // Mechanic
        helpers.add("ExtendedVariantMode"); // Other/Misc
        helpers.add("memorialHelper"); // Other/Misc
        helpers.add("NovasUtils"); // Lönn Plugin
        helpers.add("Portaline"); // Mechanic
        helpers.add("ShaderHelper"); // Other/Misc
        helpers.add("CutsceneHelper"); // Other/Misc

        // we don't want the demo map for Fancy Tile Entities, we want the helper itself
        helpers.add("FancyTileEntities");
        helpers.remove("FancyTileEntities_Demo");

        // this is an April Fools mod that's not even a helper, come on
        helpers.remove("LagHelper");

        List<String> helpersList = new ArrayList<>(helpers);
        helpersList.sort(Comparator.naturalOrder());
        helperList = new JSONArray(helpersList).toString().getBytes(StandardCharsets.UTF_8);

        log.debug("Found {} helpers in the database.", helpersList.size());
    }

    private void refreshModIDsToNamesMap() {
        Set<String> idsSharingPageWithOtherIds = new HashSet<>();
        {
            Set<String> encounteredPages = new HashSet<>();
            for (ModDatabase.ModLatestVersion record : ModDatabase.listLatestVersions(database)) {
                String page = record.mod().id;
                if (encounteredPages.contains(page)) {
                    idsSharingPageWithOtherIds.add(record.file().modId);
                } else {
                    encounteredPages.add(page);
                }
            }
            log.debug("Mod IDs found to be sharing pages with other mod IDs: {}", idsSharingPageWithOtherIds);
        }

        Map<String, Pair<String, String>> modIdsToNamesAndCategoriesMap = ModDatabase.listLatestVersions(database).stream()
                .map(entry -> {
                    String concat = "";
                    if (idsSharingPageWithOtherIds.contains(entry.file().modId)) {
                        // we want to remove version numbers because this might not be the one the user has installed.
                        StringBuilder megaregex = new StringBuilder();
                        for (int i = 1; i <= 7; i++) {
                            for (int j = 0; j < i; j++) {
                                megaregex.append('[').append("version".charAt(j)).append(Character.toUpperCase("version".charAt(j))).append(']');
                            }
                            if (i != 7) megaregex.append('|');
                        }
                        String matchFileWithoutVersions = entry.file().description.replaceAll("(" + megaregex + ")?\\.? ?([0-9]+.)*[0-9]+", "");
                        matchFileWithoutVersions = matchFileWithoutVersions.replace("[]", "").replace("()", "");
                        matchFileWithoutVersions = StringUtils.strip(matchFileWithoutVersions, " -/");
                        log.debug("Matched file description for {} / file {}: {} -> {}", entry.file().modId, entry.file().id, entry.file().description, matchFileWithoutVersions);

                        if (!matchFileWithoutVersions.isEmpty()) {
                            concat = " ∙ " + matchFileWithoutVersions;
                        }
                    }

                    CategoryRecord c = entry.mod().category;
                    while (c.parent != null) c = c.parent;
                    return Pair.of(entry.file().modId, Pair.of(entry.mod().name + concat, c.name));
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        {
            Map<String, String> modIdsToNamesMap = modIdsToNamesAndCategoriesMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, k -> k.getValue().getLeft()));
            modIdsToNames = new JSONObject(modIdsToNamesMap).toString().getBytes(StandardCharsets.UTF_8);
        }
        {
            Map<String, String> modIdsToCategoriesMap = modIdsToNamesAndCategoriesMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, k -> k.getValue().getRight()));
            modIdsToCategories = new JSONObject(modIdsToCategoriesMap).toString().getBytes(StandardCharsets.UTF_8);
        }

        log.debug("Associated {} mod IDs with their names.", modIdsToNamesAndCategoriesMap.size());
    }

    private void refreshEverestVersions() throws IOException {
        everestVersions = IOUtils.toByteArray(Files.newInputStream(Paths.get("/shared/celeste/everest-versions.json")));
        log.debug("Reloaded Everest versions! Preloaded {} bytes.", everestVersions.length);
    }

    public static ModDatabase.ModLatestVersion getModInfoByEverestYamlId(String modId) {
        return ModDatabase.listLatestVersions(database).stream()
                .filter(m -> m.file().modId.equals(modId))
                .findFirst()
                .orElse(null);
    }

    public static ModRecord getModInfoByModId(String modId) {
        return database.stream()
                .filter(m -> m.id.equals(modId))
                .findFirst()
                .orElse(null);
    }

    private void refreshCategoriesLists() throws IOException {
        List<Map<String, Object>> categories = computeCategoryList();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            YamlUtil.dump(categories, baos);
            precomputedCategoryList = baos.toByteArray();
            log.debug("Precomputed categories list! Length: {} bytes", precomputedCategoryList.length);
        }

        // welcome to generic type hell
        // categoryid => list of subcategories
        Map<String, Map<String, List<Map<String, Object>>>> subcategories = new HashMap<>();
        for (Map<String, Object> entry : categories) {
            if (!entry.containsKey("itemtype")) continue;
            String itemtype = (String) entry.get("itemtype");
            if (!subcategories.containsKey(itemtype)) subcategories.put(itemtype, new HashMap<>());

            String categoryid = (String) entry.get("categoryid");
            subcategories.get(itemtype).put(categoryid, computeSubcategoryListFor(categoryid));
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            YamlUtil.dump(subcategories, baos);
            precomputedSubcategoryList = baos.toByteArray();
            log.debug("Precomputed subcategories list! Length: {} bytes", precomputedSubcategoryList.length);
        }
    }

    private static Map<String, Object> serializeModInfo(ModRecord m) {
        Map<String, Object> contents = new TreeMap<>(ImmutableMap.of(
                "PageURL", m.pageUrl,
                "Name", m.name,
                "Author", m.author.name,
                "Description", m.summary,
                "Likes", m.likes,
                "Views", m.views,
                "Downloads", m.downloads,
                "Text", m.description
        ));
        contents.put("CreatedDate", m.createdDate);
        contents.put("ModifiedDate", m.modifiedDate);
        contents.put("UpdatedDate", m.updatedDate);
        contents.put("Screenshots", Arrays.stream(m.screenshots)
                .map(s -> s.mainUrl)
                .toList());
        contents.put("MirroredScreenshots", Arrays.stream(m.screenshots)
                .map(s -> s.mirrorName)
                .filter(Objects::nonNull)
                .map(s -> "https://celestemodupdater.0x0a.de/banana-mirror-images/" + s + ".png")
                .toList());
        contents.put("Files", Arrays.stream(m.files)
                .map(f -> ImmutableMap.of(
                        "Description", f.description,
                        "HasEverestYaml", f.hasEverestYaml,
                        "Size", f.size,
                        "CreatedDate", f.createdDate,
                        "Downloads", f.downloads,
                        "URL", f.mainUrl,
                        "Name", f.name,
                        "MirrorName", f.mirrorName
                ))
                .toList());

        Map<String, Object> recurseItem = new TreeMap<>();
        contents.put("Category", recurseItem);
        CategoryRecord recurse = new CategoryRecord();
        while (true) {
            recurseItem.put("ID", recurse.id);
            recurseItem.put("Name", recurse.name);
            if (recurse.parent == null) break;

            Map<String, Object> newRecurseItem = new TreeMap<>();
            recurseItem.put("Parent", newRecurseItem);

            recurse = recurse.parent;
            recurseItem = newRecurseItem;
        }

        // jank GameBanana-dependent mapping I need to get rid of
        CategoryRecord category = m.category, subcategory = m.category;
        while (category.parent != null && !category.parent.id.endsWith("/Root")) {
            category = category.parent;
        }
        String itemtype = "Mod";
        if (category.parent != null && category.parent.id.equals("GameBanana/Wip/Root")) {
            itemtype = "Wip";
        }
        if (category.parent != null && category.parent.id.equals("GameBanana/Tool/Root")) {
            itemtype = "Tool";
        }

        contents.putAll(ImmutableMap.of(
                "GameBananaType", itemtype,
                "CategoryId", category.id.substring(category.id.lastIndexOf("/") + 1),
                "CategoryName", category.name
        ));
        if (!category.equals(subcategory)) {
            contents.putAll(ImmutableMap.of(
                    "SubcategoryId", subcategory.id.substring(category.id.lastIndexOf("/") + 1),
                    "SubcategoryName", subcategory.name
            ));
        }
        return contents;
    }
}
