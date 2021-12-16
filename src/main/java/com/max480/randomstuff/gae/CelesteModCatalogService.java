package com.max480.randomstuff.gae;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is the servlet generating the Custom Entity Catalog page.
 */
@WebServlet(name = "CelesteModCatalogService", urlPatterns = {"/celeste/custom-entity-catalog", "/celeste/custom-entity-catalog.json"})
public class CelesteModCatalogService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModCatalogService");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/custom-entity-catalog.json")) {
            response.setHeader("Content-Type", "application/json");
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("custom_entity_catalog.json")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/custom-entity-catalog")) {
            Pair<List<QueriedModInfo>, ZonedDateTime> list = null;

            try {
                list = loadList();
            } catch (Exception e) {
                logger.severe("Could not load mod catalog: " + e);
                e.printStackTrace();
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
            request.getRequestDispatcher("/WEB-INF/mod-catalog.jsp").forward(request, response);
        } else {
            logger.warning("Not found");
            response.setStatus(404);
        }
    }

    public static String getSampleEverestYaml(QueriedModInfo mod) {
        return new Yaml().dumpAs(ImmutableList.of(
                ImmutableMap.of(
                        "Name", "YourModName",
                        "Version", "1.0.0",
                        "Dependencies", ImmutableList.of(
                                ImmutableMap.of(
                                        "Name", mod.modEverestYamlId,
                                        "Version", mod.latestVersion
                                )
                        ))
        ), null, DumperOptions.FlowStyle.BLOCK);
    }

    private Pair<List<QueriedModInfo>, ZonedDateTime> loadList() throws IOException {
        // just load and parse the custom entity catalog JSON.
        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("custom_entity_catalog.json")) {
            JSONObject obj = new JSONObject(IOUtils.toString(is, UTF_8));
            ZonedDateTime lastUpdated = ZonedDateTime.parse(obj.getString("lastUpdated")).withZoneSameInstant(ZoneId.of("UTC"));
            List<QueriedModInfo> modInfo = obj.getJSONArray("modInfo").toList().stream()
                    .map(item -> (HashMap<String, Object>) item)
                    .map(QueriedModInfo::new)
                    .collect(Collectors.toList());

            logger.fine("Loaded " + modInfo.size() + " mods.");
            return Pair.of(modInfo, lastUpdated);
        }
    }

    /**
     * A small object to hold an itemtype/itemid pair (this identifies a mod uniquely on GameBanana).
     */
    public static class QueriedModInfo {
        public String itemtype;
        public int itemid;
        public int categoryId;
        public String categoryName;
        public String modName;
        public String modEverestYamlId;
        public String latestVersion;
        public int dependentCount;
        public Map<String, List<String>> entityList;
        public Map<String, List<String>> triggerList;
        public Map<String, List<String>> effectList;
        public Map<String, String> documentationLinks;

        public QueriedModInfo(HashMap<String, Object> object) {
            itemtype = (String) object.get("itemtype");
            itemid = (int) object.get("itemid");
            categoryId = (int) object.get("categoryId");
            categoryName = (String) object.get("categoryName");
            modName = (String) object.get("modName");
            modEverestYamlId = (String) object.get("modEverestYamlId");
            latestVersion = (String) object.get("latestVersion");
            dependentCount = (int) object.get("dependentCount");
            entityList = new TreeMap<>((Map<String, List<String>>) object.get("entityList"));
            triggerList = new TreeMap<>((Map<String, List<String>>) object.get("triggerList"));
            effectList = new TreeMap<>((Map<String, List<String>>) object.get("effectList"));
            documentationLinks = new LinkedHashMap<>();

            ((ArrayList<Object>) object.get("documentationLinks")).stream()
                    .map(item -> (Map<String, Object>) item)
                    .forEach(item -> documentationLinks.put((String) item.get("key"), (String) item.get("value")));
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
