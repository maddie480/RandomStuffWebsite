package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is the servlet generating the Custom Entity Catalog page.
 */
@WebServlet(name = "CelesteModCatalogService", loadOnStartup = 1, urlPatterns = {
        "/celeste/custom-entity-catalog", "/celeste/custom-entity-catalog.json", "/celeste/custom-entity-catalog-reload"})
public class CelesteModCatalogService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModCatalogService");

    private static List<QueriedModInfo> modInfo = null;
    private static ZonedDateTime lastUpdated = null;

    @Override
    public void init() {
        try {
            reloadList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Loading mod catalog failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/custom-entity-catalog-reload")
                && ("key=" + SecretConstants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {

            reloadList();
        } else if (request.getRequestURI().equals("/celeste/custom-entity-catalog.json")) {
            response.setHeader("Content-Type", "application/json");
            try (InputStream is = CelesteModUpdateService.getCloudStorageInputStream("custom_entity_catalog.json")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/custom-entity-catalog")) {
            if (modInfo == null) {
                request.setAttribute("error", true);
            } else {
                request.setAttribute("error", false);
                request.setAttribute("mods", modInfo);
                request.setAttribute("lastUpdated", lastUpdated.format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm zzz", Locale.ENGLISH)
                ));
                request.setAttribute("modCount", modInfo.size());
                request.setAttribute("entityCount", modInfo.stream()
                        .mapToLong(modInfo -> modInfo.entityList.size())
                        .sum());
                request.setAttribute("triggerCount", modInfo.stream()
                        .mapToLong(modInfo -> modInfo.triggerList.size())
                        .sum());
                request.setAttribute("effectCount", modInfo.stream()
                        .mapToLong(modInfo -> modInfo.effectList.size())
                        .sum());
            }
            request.getRequestDispatcher("/WEB-INF/mod-catalog.jsp").forward(request, response);
        } else {
            logger.warning("Invalid key");
            response.setStatus(403);
        }
    }

    private void reloadList() throws IOException {
        // just load and parse the custom entity catalog JSON.
        try (InputStream is = CelesteModUpdateService.getCloudStorageInputStream("custom_entity_catalog.json")) {
            JSONObject obj = new JSONObject(IOUtils.toString(is, UTF_8));
            lastUpdated = ZonedDateTime.parse(obj.getString("lastUpdated")).withZoneSameInstant(ZoneId.of("UTC"));
            modInfo = obj.getJSONArray("modInfo").toList().stream()
                    .map(item -> (HashMap<String, Object>) item)
                    .map(QueriedModInfo::new)
                    .collect(Collectors.toList());

            logger.info("Loaded " + modInfo.size() + " mods.");
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
        public Set<String> entityList;
        public Set<String> triggerList;
        public Set<String> effectList;
        public Map<String, String> documentationLinks;

        public QueriedModInfo(HashMap<String, Object> object) {
            itemtype = (String) object.get("itemtype");
            itemid = (int) object.get("itemid");
            categoryId = (int) object.get("categoryId");
            categoryName = (String) object.get("categoryName");
            modName = (String) object.get("modName");
            entityList = ((ArrayList<Object>) object.get("entityList")).stream().map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
            triggerList = ((ArrayList<Object>) object.get("triggerList")).stream().map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
            effectList = ((ArrayList<Object>) object.get("effectList")).stream().map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
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
