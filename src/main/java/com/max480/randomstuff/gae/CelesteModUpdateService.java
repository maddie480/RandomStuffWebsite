package com.max480.randomstuff.gae;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet caches and provides the everest_update.yaml Everest downloads to check for updates.
 * It also provides file_ids.yaml, that can be used to get all GameBanana file IDs that belong to Celeste mods.
 */
@WebServlet(name = "CelesteModUpdateService", loadOnStartup = 1, urlPatterns = {"/celeste/everest_update.yaml",
        "/celeste/file_ids.yaml", "/celeste/everest-update-reload", "/celeste/mod_search_database.yaml",
        "/celeste/mod_files_database.zip", "/celeste/mod_dependency_graph.yaml"})
public class CelesteModUpdateService extends HttpServlet {
    private final Logger logger = Logger.getLogger("CelesteModUpdateService");

    private byte[] everestYaml;
    private String everestYamlEtag;

    @Override
    public void init() {
        try {
            logger.fine("Downloading everest_update.yaml from Cloud Storage");
            everestYaml = IOUtils.toByteArray(CloudStorageUtils.getCloudStorageInputStream("everest_update.yaml"));
            everestYamlEtag = "\"" + DigestUtils.sha512Hex(everestYaml) + "\"";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/celeste/file_ids.yaml".equals(request.getRequestURI())) {
            // transfer file_ids.yaml from backend
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("file_ids.yaml")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/everest-update-reload")
                && ("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
            // trigger a reload of everest_update.yaml
            everestYaml = IOUtils.toByteArray(CloudStorageUtils.getCloudStorageInputStream("everest_update.yaml"));
            everestYamlEtag = "\"" + DigestUtils.sha512Hex(everestYaml) + "\"";
        } else if (request.getRequestURI().equals("/celeste/everest_update.yaml")) {
            // send the everest_update.yaml we have in cache
            response.setHeader("ETag", everestYamlEtag);
            if (everestYamlEtag.equals(request.getHeader("If-None-Match"))) {
                response.setStatus(304);
            } else {
                response.setHeader("Content-Type", "text/yaml");
                IOUtils.write(everestYaml, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/mod_search_database.yaml")) {
            // send mod_search_database.yaml from Cloud Storage
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("mod_search_database.yaml")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/mod_files_database.zip")) {
            // send mod_files_database.zip from Cloud Storage
            response.setHeader("Content-Type", "application/zip");
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("mod_files_database.zip")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/mod_dependency_graph.yaml")) {
            // send mod_dependency_graph.yaml from Cloud Storage
            boolean everestYamlFormat = "everestyaml".equals(request.getParameter("format"));
            response.setHeader("Content-Type", "text/yaml");

            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream(
                    everestYamlFormat ? "mod_dependency_graph_everest.yaml" : "mod_dependency_graph.yaml")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else {
            logger.warning("Invalid key");
            response.setStatus(403);
        }
    }
}
