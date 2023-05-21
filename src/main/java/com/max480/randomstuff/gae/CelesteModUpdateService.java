package com.max480.randomstuff.gae;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This servlet caches and provides the everest_update.yaml Everest downloads to check for updates.
 * It also provides file_ids.yaml, that can be used to get all GameBanana file IDs that belong to Celeste mods.
 */
@WebServlet(name = "CelesteModUpdateService", loadOnStartup = 1, urlPatterns = {"/celeste/everest_update.yaml",
        "/celeste/everest-update-reload", "/celeste/mod_search_database.yaml", "/celeste/mod_files_database.zip",
        "/celeste/mod_dependency_graph.yaml"})
public class CelesteModUpdateService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteModUpdateService.class);

    private byte[] everestYaml;

    @Override
    public void init() {
        try {
            log.debug("Reading everest_update.yaml from storage");
            everestYaml = IOUtils.toByteArray(Files.newInputStream(Paths.get("/shared/celeste/updater/everest-update.yaml")));
            CelesteDirectURLService.updateUrls();
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/celeste/file_ids.yaml".equals(request.getRequestURI())) {
            // transfer file_ids.yaml from backend
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/file-ids.yaml"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/everest-update-reload")
                && ("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
            // trigger a reload of everest_update.yaml
            everestYaml = IOUtils.toByteArray(Files.newInputStream(Paths.get("/shared/celeste/updater/everest-update.yaml")));
            CelesteDirectURLService.updateUrls();
        } else if (request.getRequestURI().equals("/celeste/everest_update.yaml")) {
            // send the everest_update.yaml we have in cache
            response.setHeader("Content-Type", "text/yaml");
            IOUtils.write(everestYaml, response.getOutputStream());
        } else if (request.getRequestURI().equals("/celeste/mod_search_database.yaml")) {
            // send mod_search_database.yaml from storage
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/mod-search-database.yaml"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/mod_files_database.zip")) {
            // send mod_files_database.zip from storage
            response.setHeader("Content-Type", "application/zip");
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/mod-files-database.zip"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/mod_dependency_graph.yaml")) {
            // send mod_dependency_graph.yaml from storage
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/mod-dependency-graph.yaml"))) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else {
            log.warn("Invalid key");
            response.setStatus(403);
        }
    }
}
