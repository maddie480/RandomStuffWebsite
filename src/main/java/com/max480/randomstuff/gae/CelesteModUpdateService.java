package com.max480.randomstuff.gae;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.IOUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet caches and provides the everest_update.yaml Everest downloads to check for updates.
 * It also provides file_ids.yaml, that can be used to get all GameBanana file IDs that belong to Celeste mods.
 */
@WebServlet(name = "CelesteModUpdateService", loadOnStartup = 3, urlPatterns = {"/celeste/everest_update.yaml",
        "/celeste/file_ids.yaml", "/celeste/everest-update-reload"})
public class CelesteModUpdateService extends HttpServlet {
    private final Logger logger = Logger.getLogger("CelesteModUpdateService");

    private byte[] everestYaml;

    @Override
    public void init() {
        try {
            logger.fine("Downloading everest_update.yaml from Cloud Storage");

            Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();
            BlobId blobId = BlobId.of("max480-random-stuff.appspot.com", "everest_update_backup.yaml");
            everestYaml = storage.get(blobId).getContent();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/celeste/file_ids.yaml".equals(request.getRequestURI())) {
            // transfer file_ids.yaml from backend
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = getConnectionWithTimeouts(Constants.FILE_IDS_URL).getInputStream()) {
                IOUtils.copy(is, response.getOutputStream());
            }
        } else if (request.getRequestURI().equals("/celeste/everest-update-reload")
                && ("key=" + Constants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
            // trigger a reload of everest_update.yaml
            reloadEverestUpdate();
        } else if (request.getRequestURI().equals("/celeste/everest_update.yaml")) {
            // send the everest_update.yaml we have in cache
            response.setHeader("Content-Type", "text/yaml");
            IOUtils.write(everestYaml, response.getOutputStream());
        } else {
            response.setStatus(404);
        }
    }

    private void reloadEverestUpdate() throws IOException {
        logger.fine("Refreshing everest_update.yaml from backend server and backing up on Cloud Storage");

        // try contacting the server and cache the response.
        byte[] yaml = IOUtils.toByteArray(getConnectionWithTimeouts(Constants.UPDATE_CHECKER_SERVER_URL));

        // back up on Cloud Storage.
        Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();
        BlobId blobId = BlobId.of("max480-random-stuff.appspot.com", "everest_update_backup.yaml");
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/yaml").build();
        storage.create(blobInfo, yaml);

        everestYaml = yaml;
    }

    public static URLConnection getConnectionWithTimeouts(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(10000);
        return connection;
    }
}
