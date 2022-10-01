package com.max480.randomstuff.gae;

import com.google.api.gax.batching.BatchingSettings;
import com.google.cloud.ReadChannel;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service allowing to trigger and retrieve results of file searches across all Celeste mods.
 */
@WebServlet(name = "CelesteFileSearchService", loadOnStartup = 8, urlPatterns = {"/celeste/file-search"})
public class CelesteFileSearchService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("CelesteFileSearchService");
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    private static final Set<String> pendingSearches = new HashSet<>();

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
        String query = request.getParameter("query");
        String exact = request.getParameter("exact");

        if (query == null || query.isEmpty() || !Arrays.asList("true", "false").contains(exact)) {
            response.setStatus(400);
            response.setHeader("Content-Type", "text/plain");
            response.getWriter().write("'query' and 'exact' parameters are missing or invalid!");
            return;
        }

        response.setHeader("Content-Type", "application/json");

        String fileName = "file_searches/" + URLEncoder.encode(query.toLowerCase(Locale.ROOT), "UTF-8") + "_" + exact + ".json";
        BlobId blobId = BlobId.of("staging.max480-random-stuff.appspot.com", fileName);

        if (storage.get(blobId) != null) {
            logger.fine(fileName + " is done, returning results!");

            // copy search results from Cloud Storage
            try (ReadChannel reader = storage.reader(blobId);
                 WritableByteChannel writer = Channels.newChannel(response.getOutputStream())) {

                ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
                while (reader.read(buffer) > 0 || buffer.position() != 0) {
                    buffer.flip();
                    writer.write(buffer);
                    buffer.compact();
                }
            }

            pendingSearches.remove(fileName);
        } else {
            // result does not exist yet!
            if (!pendingSearches.contains(fileName)) {
                // request the search to the backend through Pub/Sub
                JSONObject message = new JSONObject();
                message.put("taskType", "fileSearch");
                message.put("search", query);
                message.put("exact", "true".equals(exact));

                ByteString data = ByteString.copyFromUtf8(message.toString());
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

                try {
                    String messageId = publisher.publish(pubsubMessage).get();
                    logger.info("Emitted message id " + messageId + " to handle " + fileName + "!");
                } catch (InterruptedException | ExecutionException ex) {
                    throw new IOException(ex);
                }

                pendingSearches.add(fileName);
            }

            logger.fine(fileName + " is in progress");
            response.getWriter().write("{\"pending\": true}");
        }
    }
}
