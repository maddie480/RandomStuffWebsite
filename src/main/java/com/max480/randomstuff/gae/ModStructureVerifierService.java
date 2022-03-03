package com.max480.randomstuff.gae;

import com.google.api.gax.batching.BatchingSettings;
import com.google.cloud.WriteChannel;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Servlet allowing to submit files to the Mod Structure Verifier bot.
 */
@WebServlet(name = "ModStructureVerifierService", loadOnStartup = 5, urlPatterns = {"/celeste/mod-structure-verifier"})
@MultipartConfig
public class ModStructureVerifierService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("ModStructureVerifierService");
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();
    private Publisher publisher = null;

    @Override
    public void init() {
        try {
            publisher = Publisher.newBuilder(TopicName.of("max480-random-stuff", "backend-tasks"))
                    .setBatchingSettings(BatchingSettings.newBuilder().setIsEnabled(false).build())
                    .build();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Building the Pub/Sub publisher for Font Generator failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("badrequest", false);
        request.getRequestDispatcher("/WEB-INF/mod-structure-verifier.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("badrequest", false);

        if (!ServletFileUpload.isMultipartContent(request)) {
            // if not, we stop here
            request.setAttribute("badrequest", true);
            logger.warning("Bad request");
            response.setStatus(400);
        } else {
            ServletFileUpload upload = new ServletFileUpload();

            logger.fine("Parsing request...");

            String id = UUID.randomUUID().toString();

            // parse request
            boolean modZipFound = false;
            String assetsFolderName = null;
            String mapsFolderName = null;

            try {
                FileItemIterator iter = upload.getItemIterator(request);
                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    if (item.isFormField()) {
                        // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                        String fieldname = item.getFieldName();
                        String fieldvalue;
                        try (InputStream is = item.openStream()) {
                            fieldvalue = IOUtils.toString(is, UTF_8);
                        }

                        if ("assetFolderName".equals(fieldname)) {
                            assetsFolderName = fieldvalue;
                        } else if ("mapFolderName".equals(fieldname)) {
                            mapsFolderName = fieldvalue;
                        }
                    } else {
                        // Process form file field (input type="file").
                        String fieldname = item.getFieldName();
                        if ("zipFile".equals(fieldname)) {
                            modZipFound = true;

                            // stream file straight into Cloud Storage.
                            BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", "mod-structure-verify-" + id + ".zip");
                            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();

                            try (InputStream readerStream = item.openStream();
                                 WriteChannel writer = storage.writer(blobInfo);
                                 ReadableByteChannel reader = Channels.newChannel(readerStream)) {

                                ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
                                while (reader.read(buffer) > 0 || buffer.position() != 0) {
                                    buffer.flip();
                                    writer.write(buffer);
                                    buffer.compact();
                                }
                            }
                        }
                    }
                }
            } catch (FileUploadException e) {
                logger.warning("Cannot parse request: " + e);
            }

            if (!modZipFound
                    || (mapsFolderName != null && !mapsFolderName.matches("^[A-Za-z0-9]*$"))
                    || (assetsFolderName != null && !assetsFolderName.matches("^[A-Za-z0-9]*$"))) {

                request.setAttribute("badrequest", true);
                logger.warning("Bad request: no file was sent in POST request, or one of the folder names has forbidden characters");
                response.setStatus(400);
            } else {
                // create the task, and redirect to the page that will allow to follow it
                runModStructureVerifyTask(id, assetsFolderName, mapsFolderName);
                response.setStatus(302);
                response.setHeader("Location", "/celeste/task-tracker/mod-structure-verify/" + id);
                return;
            }
        }

        request.getRequestDispatcher("/WEB-INF/mod-structure-verifier.jsp").forward(request, response);
    }

    private void runModStructureVerifyTask(String id, String assetsFolderName, String mapsFolderName) throws IOException {
        // send timestamp marker to Cloud Storage (this will save that the task exists, and the timestamp at which it started)
        BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", "mod-structure-verify-" + id + "-timestamp.txt");
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
        storage.create(blobInfo, Long.toString(System.currentTimeMillis()).getBytes(UTF_8));

        logger.fine("Creating task...");

        // generate message payload
        JSONObject message = new JSONObject();
        message.put("taskType", "modStructureVerify");
        message.put("fileName", "mod-structure-verify-" + id + ".zip");

        if (assetsFolderName == null || assetsFolderName.isEmpty()) {
            // no path was given
            message.put("withPathsCheck", false);
        } else {
            // paths were given: enable paths check and pass parameters
            message.put("withPathsCheck", true);
            message.put("assetFolderName", assetsFolderName);

            if (mapsFolderName == null || mapsFolderName.isEmpty()) {
                // map folder name = asset folder name
                message.put("mapFolderName", assetsFolderName);
            } else {
                message.put("mapFolderName", mapsFolderName);
            }
        }

        // publish the message to the backend!
        ByteString data = ByteString.copyFromUtf8(message.toString());
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

        try {
            String messageId = publisher.publish(pubsubMessage).get();
            logger.info("Emitted message id " + messageId + " to handle font generation task " + id + "!");
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }
}
