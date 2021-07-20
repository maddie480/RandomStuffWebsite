package com.max480.randomstuff.gae;

import com.google.cloud.storage.*;
import org.apache.commons.io.IOUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "CelesteModUpdateService", loadOnStartup = 1, urlPatterns = {"/celeste/everest_update.yaml", "/celeste/file_ids.yaml"})
public class CelesteModUpdateService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModUpdateService");

    private byte[] cachedYaml;
    private ZonedDateTime cachedYamlValidUntil = ZonedDateTime.now().minusMonths(1);
    private ZonedDateTime lastBackupValidUntil = ZonedDateTime.now().minusMonths(1);

    @Override
    public void init() {
        try {
            logger.log(Level.INFO, "Warmup: everest_update.yaml from server is " +
                    IOUtils.toByteArray(getConnectionWithTimeouts(Constants.UPDATE_CHECKER_SERVER_URL)).length + " bytes long");
            logger.log(Level.INFO, "Warmup: Backed up everest_update.yaml is " +
                    IOUtils.toByteArray(getConnectionWithTimeouts("https://storage.googleapis.com/max480-random-stuff.appspot.com/" + Constants.CLOUD_STORAGE_BACKUP_FILENAME)).length + " bytes long");
            logger.log(Level.INFO, "Warmup: file_ids.yaml is " +
                    IOUtils.toByteArray(getConnectionWithTimeouts(Constants.FILE_IDS_URL)).length + " bytes long");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/yaml");

        if ("/celeste/file_ids.yaml".equals(request.getRequestURI())) {
            try (InputStream is = getConnectionWithTimeouts(Constants.FILE_IDS_URL).getInputStream()) {
                IOUtils.copy(is, response.getOutputStream());
            }
            return;
        }

        if (cachedYamlValidUntil.isAfter(ZonedDateTime.now())) {
            // last request to server was < 5 minutes ago, just return the same response.
            IOUtils.write(cachedYaml, response.getOutputStream());
        } else {
            logger.fine("Cached yaml expired, contacting server");

            try {
                // try contacting the server and send the response to the caller.
                byte[] yaml = IOUtils.toByteArray(getConnectionWithTimeouts(Constants.UPDATE_CHECKER_SERVER_URL));
                IOUtils.write(yaml, response.getOutputStream());

                // cache the response for the next 5 minutes.
                cachedYaml = yaml;
                cachedYamlValidUntil = ZonedDateTime.now().plusMinutes(5);

                if (lastBackupValidUntil.isBefore(ZonedDateTime.now())) {
                    // the last time we updated the backup on Cloud Storage was more than an hour ago, so update it again.
                    logger.fine("Cloud Storage backup expired, refreshing it");

                    // we want to make the backup next time the minute hits 50 for... reasons
                    lastBackupValidUntil = ZonedDateTime.now().withMinute(50);
                    if (lastBackupValidUntil.isBefore(ZonedDateTime.now())) {
                        lastBackupValidUntil = lastBackupValidUntil.plusHours(1);
                    }

                    // do it asynchronously so that it doesn't slow down the response.
                    new Thread(() -> {
                        Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();
                        BlobId blobId = BlobId.of("max480-random-stuff.appspot.com", Constants.CLOUD_STORAGE_BACKUP_FILENAME);
                        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/yaml").build();
                        storage.create(blobInfo, yaml);
                        storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
                    }).start();
                }
            } catch (IOException e) {
                // if server doesn't respond, we should send the Google Cloud Storage backup instead.
                logger.log(Level.WARNING, "Server unreachable, falling back to backup: " + e.toString());
                byte[] yaml = IOUtils.toByteArray(getConnectionWithTimeouts("https://storage.googleapis.com/max480-random-stuff.appspot.com/" + Constants.CLOUD_STORAGE_BACKUP_FILENAME));
                IOUtils.write(yaml, response.getOutputStream());
            }
        }
    }

    public static URLConnection getConnectionWithTimeouts(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(10000);
        return connection;
    }
}
