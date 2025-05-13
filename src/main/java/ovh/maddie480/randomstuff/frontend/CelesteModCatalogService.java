package ovh.maddie480.randomstuff.frontend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is the servlet generating the Custom Entity Catalog page.
 */
@WebServlet(name = "CelesteModCatalogService", urlPatterns = {"/celeste/custom-entity-catalog",
        "/celeste/custom-entity-catalog.json", "/celeste/custom-entity-dictionary.csv"})
public class CelesteModCatalogService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteModCatalogService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/custom-entity-catalog.json")) {
            response.setHeader("Content-Type", "application/json");
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/custom-entity-catalog.json"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/custom-entity-dictionary.csv")) {
            response.setHeader("Content-Type", "text/csv");
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/custom-entity-dictionary.csv"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/custom-entity-catalog")) {
            Pair<List<QueriedModInfo>, ZonedDateTime> list = null;

            try {
                list = loadList();
            } catch (Exception e) {
                log.error("Could not load mod catalog!", e);
            }


            if (list == null) {
                request.setAttribute("error", true);
            } else {
                List<QueriedModInfo> modInfo = list.getLeft();
                ZonedDateTime lastUpdated = list.getRight();

                request.setAttribute("error", false);
                request.setAttribute("mods", modInfo);
                request.setAttribute("lastUpdated", lastUpdated.format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm zzz", Locale.ENGLISH)
                ));
                request.setAttribute("lastUpdatedTimestamp", lastUpdated.toEpochSecond());
                request.setAttribute("modCount", modInfo.size());
                request.setAttribute("entityCount", modInfo.stream()
                        .mapToLong(info -> info.entityList.size())
                        .sum());
                request.setAttribute("triggerCount", modInfo.stream()
                        .mapToLong(info -> info.triggerList.size())
                        .sum());
                request.setAttribute("effectCount", modInfo.stream()
                        .mapToLong(info -> info.effectList.size())
                        .sum());
            }
            PageRenderer.render(request, response, "mod-catalog", "Celeste Custom Entity and Trigger List",
                    "A big list containing all custom entities and triggers from mods published on GameBanana.");
        } else {
            log.warn("Not found");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        }
    }

    public static String getSampleEverestYaml(QueriedModInfo mod) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            YamlUtil.dump(ImmutableList.of(
                    ImmutableMap.of(
                            "Name", "YourModName",
                            "Version", "1.0.0",
                            "Dependencies", ImmutableList.of(
                                    ImmutableMap.of(
                                            "Name", mod.modEverestYamlId,
                                            "Version", mod.latestVersion
                                    )
                            ))
            ), os);
            return os.toString(UTF_8);
        }
    }

    private Pair<List<QueriedModInfo>, ZonedDateTime> cachedList = null;
    private FileTime cachedListLastModified = FileTime.fromMillis(0);

    private Pair<List<QueriedModInfo>, ZonedDateTime> loadList() throws IOException {
        // just load and parse the custom entity catalog JSON.
        Path json = Paths.get("/shared/celeste/custom-entity-catalog.json");
        FileTime lastModified = Files.getLastModifiedTime(json);
        if (lastModified.equals(cachedListLastModified)) {
            return cachedList;
        }

        log.info("Reloading custom entity catalog because last modified date changed: {} -> {}", cachedListLastModified, lastModified);
        try (BufferedReader br = Files.newBufferedReader(json)) {
            JSONObject obj = new JSONObject(new JSONTokener(br));
            ZonedDateTime lastUpdated = ZonedDateTime.parse(obj.getString("lastUpdated")).withZoneSameInstant(ZoneId.of("UTC"));
            Map<String, Map<String, Map<String, String>>> entityDescriptions = (Map) obj.getJSONObject("entityDescriptions").toMap();
            List<QueriedModInfo> modInfo = obj.getJSONArray("modInfo").toList().stream()
                    .map(item -> (HashMap<String, Object>) item)
                    .map(item -> new QueriedModInfo(item, entityDescriptions))
                    .collect(Collectors.toList());

            log.debug("Loaded {} mods.", modInfo.size());

            Pair<List<QueriedModInfo>, ZonedDateTime> computedValue = Pair.of(modInfo, lastUpdated);
            cachedList = computedValue;
            cachedListLastModified = lastModified;
            return computedValue;
        }
    }

    /**
     * A small object to hold an itemtype/itemid pair (this identifies a mod uniquely on GameBanana).
     */
    public static class QueriedModInfo {
        public final String itemtype;
        public final int itemid;
        public final int categoryId;
        public final String categoryName;
        public final String modName;
        public final String modEverestYamlId;
        public final String latestVersion;
        public final int dependentCount;
        public final Map<Map<String, String>, List<String>> entityList;
        public final Map<Map<String, String>, List<String>> triggerList;
        public final Map<Map<String, String>, List<String>> effectList;
        public final Map<String, String> documentationLinks;

        public QueriedModInfo(HashMap<String, Object> object, Map<String, Map<String, Map<String, String>>> entityDescriptions) {
            itemtype = (String) object.get("itemtype");
            itemid = (int) object.get("itemid");
            categoryId = (int) object.get("categoryId");
            categoryName = (String) object.get("categoryName");
            modName = (String) object.get("modName");
            modEverestYamlId = (String) object.get("modEverestYamlId");
            latestVersion = (String) object.get("latestVersion");
            dependentCount = (int) object.get("dependentCount");
            entityList = mapEntityList((Map<String, List<String>>) object.get("entityList"), entityDescriptions);
            triggerList = mapEntityList((Map<String, List<String>>) object.get("triggerList"), entityDescriptions);
            effectList = mapEntityList((Map<String, List<String>>) object.get("effectList"), entityDescriptions);
            documentationLinks = new LinkedHashMap<>();

            ((ArrayList<Object>) object.get("documentationLinks")).stream()
                    .map(item -> (Map<String, Object>) item)
                    .forEach(item -> documentationLinks.put((String) item.get("key"), (String) item.get("value")));
        }

        private Map<Map<String, String>, List<String>> mapEntityList(Map<String, List<String>> entityList, Map<String, Map<String, Map<String, String>>> entityDescriptions) {
            return entityList.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().toLowerCase()))
                    .map(entity -> {
                        String entityName = entity.getKey();
                        Map<String, String> descriptionDictionary = entityDescriptions
                                .getOrDefault(itemtype + "/" + itemid, Collections.emptyMap())
                                .getOrDefault(entityName, Collections.emptyMap());
                        Map<String, String> result = new LinkedHashMap<>();
                        for (String s : entityName.split(" / ")) {
                            String entry = descriptionDictionary.get(s);
                            if (entry != null) entry = entry.replace("\\n", "\n");
                            result.put(s, entry);
                        }
                        if (!result.values().stream().allMatch(Objects::isNull)) {
                            return Pair.of(result, entity.getValue());
                        }
                        result = new HashMap<>();
                        result.put(entityName, null);
                        return Pair.of(result, entity.getValue());
                    })
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (a, b) -> a, LinkedHashMap::new));
        }
    }

    /**
     * Replaces everything that's not a letter or number with '-' in the input.
     */
    public static String dasherize(String input) {
        StringBuilder builder = new StringBuilder();

        boolean lastIsDash = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                lastIsDash = false;
                builder.append(Character.toLowerCase(c));
            } else if (!lastIsDash) {
                lastIsDash = true;
                builder.append('-');
            }
        }

        return builder.toString();
    }
}
