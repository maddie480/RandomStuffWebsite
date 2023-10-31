package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
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

@WebServlet(name = "AssetDriveService", urlPatterns = {"/celeste/asset-drive/list/decals", "/celeste/asset-drive/list/stylegrounds",
        "/celeste/asset-drive/list/fgtilesets", "/celeste/asset-drive/list/bgtilesets", "/celeste/asset-drive/list/misc", "/celeste/asset-drive/files/*"})
public class AssetDriveService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(AssetDriveService.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().startsWith("/celeste/asset-drive/list/")) {
            JSONObject allCategories;
            try (InputStream is = Files.newInputStream(Paths.get("/shared/temp/asset-drive/categorized-assets.json"))) {
                allCategories = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            }
            resp.setContentType("application/json");
            resp.getWriter().write(allCategories.getJSONArray(req.getRequestURI().substring(26)).toString());
            return;
        }

        if (req.getRequestURI().startsWith("/celeste/asset-drive/files/")) {
            JSONArray allFiles;
            try (InputStream is = Files.newInputStream(Paths.get("/shared/temp/asset-drive/cached-list.json"))) {
                allFiles = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
            }

            String fileId = req.getRequestURI().substring(27);

            JSONObject foundFile = null;
            for (Object o : allFiles) {
                JSONObject file = (JSONObject) o;
                if (fileId.equals(file.getString("id"))) {
                    foundFile = file;
                    break;
                }
            }

            log.debug("Looking for asset with id {}, found: {}", fileId, foundFile);

            if (foundFile != null) {
                resp.setContentType(foundFile.getString("mimeType"));
                resp.setHeader("Content-Disposition", "Content-Disposition: attachment; filename=\"" + foundFile.getString("name") + "\"");

                try (InputStream is = downloadFileWithCache(fileId)) {
                    IOUtils.copy(is, resp.getOutputStream());
                    return;
                } catch (IOException e) {
                    // get rid of the cached file, since it might be incomplete
                    Path cached = Paths.get("/shared/temp/asset-drive/cached-" + fileId + ".bin");
                    if (Files.exists(cached)) {
                        log.warn("Deleting cached file {} due to I/O exception", cached);
                        Files.delete(cached);
                    }

                    throw e;
                }
            }
        }

        log.warn("Not found");
        resp.setStatus(404);
        PageRenderer.render(req, resp, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }

    private static InputStream downloadFileWithCache(String fileId) throws IOException {
        Path cached = Paths.get("/shared/temp/asset-drive/cached-" + fileId + ".bin");
        if (Files.exists(cached)) {
            return Files.newInputStream(cached);
        } else {
            log.debug("Downloading non-cached file with id {}", fileId);
            return new TeeInputStream(
                    ConnectionUtils.openStreamWithTimeout("https://www.googleapis.com/drive/v3/files/" + fileId + "?key=" + SecretConstants.GOOGLE_DRIVE_API_KEY + "&alt=media"),
                    Files.newOutputStream(cached)
            );
        }
    }
}
