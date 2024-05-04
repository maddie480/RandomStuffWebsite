package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Service allowing to trigger and retrieve results of file searches across all Celeste mods.
 */
@WebServlet(name = "CelesteFileSearchService", urlPatterns = {"/celeste/file-search"})
public class CelesteFileSearchService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteFileSearchService.class);

    private static final Set<String> pendingSearches = new HashSet<>();


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = request.getParameter("query");
        String exact = request.getParameter("exact");

        if (query == null || query.isEmpty() || !Arrays.asList("true", "false").contains(exact)) {
            response.setStatus(400);
            response.setHeader("Content-Type", "text/plain");
            response.getWriter().write("'query' and 'exact' parameters are missing or invalid!");
            return;
        }

        response.setHeader("Content-Type", "application/json");

        Path file = Paths.get("/shared/temp/file-searches/" + URLEncoder.encode(query.toLowerCase(Locale.ROOT), StandardCharsets.UTF_8) + "_" + exact + ".json");

        if (Files.exists(file)) {
            log.debug("{} is done, returning results!", file);

            // copy search results
            Files.copy(file, response.getOutputStream());
            pendingSearches.remove(file.toString());
        } else {
            // result does not exist yet!
            if (!pendingSearches.contains(file.toString())) {
                // request the search to the backend
                JSONObject message = new JSONObject();
                message.put("taskType", "fileSearch");
                message.put("search", query);
                message.put("exact", "true".equals(exact));

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("127.0.1.1", 44480));
                    try (OutputStream os = socket.getOutputStream();
                         OutputStreamWriter bw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {

                        message.write(bw);
                    }
                }

                pendingSearches.add(file.toString());
            }

            log.debug("{} is in progress", file);
            response.getWriter().write("{\"pending\": true}");
        }
    }
}
