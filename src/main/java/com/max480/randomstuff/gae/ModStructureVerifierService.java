package com.max480.randomstuff.gae;

import com.google.api.gax.batching.BatchingSettings;
import com.google.cloud.WriteChannel;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.*;
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
            logger.log(Level.WARNING, "Building the Pub/Sub publisher for Mod Structure Verifier failed: " + e);
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
            String chunkIndex = null;
            String chunkId = null;

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
                        } else if ("chunkId".equals(fieldname)) {
                            chunkId = fieldvalue;
                        } else if ("chunkIndex".equals(fieldname)) {
                            chunkIndex = fieldvalue;
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
                if (chunkIndex != null) {
                    chunkId = handleChunkRequest("mod-structure-verify-" + id + ".zip", chunkIndex, chunkId);

                    if (chunkId == null) {
                        // chunk request was invalid
                        response.setStatus(400);
                        return;
                    } else if (!"31".equals(chunkIndex)) {
                        // didn't send the last chunk, respond with chunk ID
                        response.setContentType("text/plain; charset=UTF-8");
                        response.getWriter().write(chunkId);
                        return;
                    }
                }

                // create the task, and give the URL to the page that will allow to follow it
                // (at this point, we either have all 32 chunks uploaded, or the sending was not chunked)
                runModStructureVerifyTask(id, chunkId, assetsFolderName, mapsFolderName);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write("/celeste/task-tracker/mod-structure-verify/" + id);
                return;
            }
        }

        request.getRequestDispatcher("/WEB-INF/mod-structure-verifier.jsp").forward(request, response);
    }

    private void runModStructureVerifyTask(String id, String chunkId, String assetsFolderName, String mapsFolderName) throws IOException {
        // send timestamp marker to Cloud Storage (this will save that the task exists, and the timestamp at which it started)
        BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", "mod-structure-verify-" + id + "-timestamp.txt");
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
        storage.create(blobInfo, Long.toString(System.currentTimeMillis()).getBytes(UTF_8));

        if (chunkId != null) {
            // merge the chunks together, there should be exactly 32 of them
            logger.fine("Composing chunks with id " + chunkId + "...");

            BlobId mergedBlobId = BlobId.of("staging.max480-random-stuff.appspot.com", "mod-structure-verify-" + id + ".zip");
            BlobInfo mergedBlobInfo = BlobInfo.newBuilder(mergedBlobId).setContentType("application/zip").build();

            final Storage.ComposeRequest.Builder builder = Storage.ComposeRequest.newBuilder();
            for (int i = 0; i < 32; i++) {
                builder.addSource("upload-chunk-" + chunkId + "-" + i + ".bin");
            }
            storage.compose(builder.setTarget(mergedBlobInfo).build());
        }

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
            logger.info("Emitted message id " + messageId + " to handle mod structure verification task " + id + "!");
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    private String handleChunkRequest(String gcsFileName, String chunkIndex, String chunkId) {
        if ("0".equals(chunkIndex)) {
            // generate a new chunk upload
            chunkId = UUID.randomUUID().toString();
        } else if (chunkId == null) {
            logger.warning("No chunk id was given and this is not the first chunk!");
            return null;
        } else if (!chunkId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            logger.warning("Chunk ID is not a UUID: " + chunkId);
            return null;
        } else {
            // check if the chunk index is in bounds and if the previous chunk exists.
            try {
                int chunkIndexInt = Integer.parseInt(chunkIndex);
                if (chunkIndexInt < 1 || chunkIndexInt > 31) {
                    logger.warning("Chunk index is outside of the allowed bounds: " + chunkIndexInt);
                    return null;
                } else {
                    Blob previousChunk = storage.get(
                            BlobId.of("staging.max480-random-stuff.appspot.com", "upload-chunk-" + chunkId + "-" + (chunkIndexInt - 1) + ".bin"));

                    if (previousChunk == null) {
                        logger.warning("Previous chunk upload-chunk-" + chunkId + "-" + (chunkIndexInt - 1) + ".bin does not exist!");
                        return null;
                    }
                }
            } catch (NumberFormatException e) {
                logger.warning("Chunk index is not an integer: " + chunkIndex);
                return null;
            }
        }

        // it seems renaming is not a thing on Cloud Storage... oh well.
        // (we are not deleting the original file because that takes time, and the bucket has a rule where any file older than 1 day is deleted.)
        logger.fine("Copying file to expected location...");
        BlobId uploadedFile = BlobId.of("staging.max480-random-stuff.appspot.com", gcsFileName);
        BlobId targetFile = BlobId.of("staging.max480-random-stuff.appspot.com", "upload-chunk-" + chunkId + "-" + chunkIndex + ".bin");
        storage.copy(Storage.CopyRequest.of(uploadedFile, targetFile));

        return chunkId;
    }
}
