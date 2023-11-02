package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AssetDriveService", loadOnStartup = 11, urlPatterns = {"/celeste/asset-drive/reload", "/celeste/asset-drive/list/decals",
        "/celeste/asset-drive/list/stylegrounds", "/celeste/asset-drive/list/fgtilesets", "/celeste/asset-drive/list/bgtilesets",
        "/celeste/asset-drive/list/misc", "/celeste/asset-drive/files/*"})
public class AssetDriveService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(AssetDriveService.class);

    private Map<String, JSONObject> fileAssetMap = Collections.emptyMap();

    @Override
    public void init() {
        try {
            buildAssetMap();
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().equals("/celeste/asset-drive/reload")) {
            if (req.getQueryString() == null || !req.getQueryString().equals("key=" + SecretConstants.RELOAD_SHARED_SECRET)) {
                // invalid secret
                log.warn("Invalid key");
                resp.setStatus(403);
            } else {
                buildAssetMap();
            }
            return;
        }

        if (req.getRequestURI().startsWith("/celeste/asset-drive/list/")) {
            JSONObject allCategories;
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/asset-drive/categorized-assets.json"))) {
                allCategories = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            }
            resp.setContentType("application/json");
            resp.getWriter().write(allCategories.getJSONArray(req.getRequestURI().substring(26)).toString());
            return;
        }

        if (req.getRequestURI().startsWith("/celeste/asset-drive/files/")) {
            String fileId = req.getRequestURI().substring(27);
            JSONObject foundFile = fileAssetMap.getOrDefault(fileId, null);
            log.debug("Looking for asset with id {}, found: {}", fileId, foundFile);

            if (foundFile != null) {
                String extension = switch (foundFile.getString("mimeType")) {
                    case "image/png" -> "png";
                    case "text/plain" -> "txt";
                    case "text/yaml" -> "yaml";
                    default -> "bin";
                };
                Path file = Paths.get("/shared/celeste/asset-drive/files/" + fileId + "." + extension);

                if (Files.exists(file)) {
                    resp.setContentType(foundFile.getString("mimeType"));
                    resp.setHeader("Content-Disposition", "Content-Disposition: attachment; filename=\"" + foundFile.getString("name") + "\"");

                    try (InputStream is = Files.newInputStream(file)) {
                        IOUtils.copy(is, resp.getOutputStream());
                        return;
                    }
                }
            }
        }

        log.warn("Not found");
        resp.setStatus(404);
        PageRenderer.render(req, resp, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }

    private void buildAssetMap() throws IOException {
        JSONArray allFiles;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/asset-drive/file-list.json"))) {
            allFiles = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        Map<String, JSONObject> result = new HashMap<>();

        for (Object o : allFiles) {
            JSONObject file = (JSONObject) o;
            result.put(file.getString("id"), file);
        }

        log.debug("Loaded asset map with {} files.", result.size());

        fileAssetMap = result;
    }
}
