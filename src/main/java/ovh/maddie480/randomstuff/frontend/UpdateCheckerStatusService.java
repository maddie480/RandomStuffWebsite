package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A status page for the Update Checker, that determines the status by reading the Google Cloud Logging logs
 * of the backend server.
 */
@WebServlet(name = "UpdateCheckerStatus", urlPatterns = {"/celeste/update-checker-status", "/celeste/update-checker-status.json"})
@MultipartConfig
public class UpdateCheckerStatusService extends HttpServlet {
    public static class LatestUpdatesEntry {
        public final boolean isAddition;
        public final String name;
        public final String version;
        public final String url;
        public final String date;
        public final long timestamp;

        public LatestUpdatesEntry(JSONObject object) {
            this.isAddition = object.getBoolean("isAddition");
            this.name = object.getString("name");
            this.version = object.getString("version");
            this.url = object.getString("url");
            this.date = object.getString("date");
            this.timestamp = object.getLong("timestamp");
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final boolean isWidget = "true".equals(request.getParameter("widget"));

        // the service is down until proven otherwise.
        request.setAttribute("up", false);

        long lastFullCheckTimestamp;
        long lastIncrementalCheckTimestamp;
        int lastFullCheckDuration;
        int lastIncrementalCheckDuration;
        List<LatestUpdatesEntry> latestUpdatesEntries;
        JSONObject updaterStatusJson;

        // retrieve the update checker status from storage!
        try (BufferedReader br = Files.newBufferedReader(Paths.get("/shared/celeste/updater/status.json"))) {
            updaterStatusJson = new JSONObject(new JSONTokener(br));

            lastFullCheckTimestamp = updaterStatusJson.getLong("lastFullCheckTimestamp");
            lastIncrementalCheckTimestamp = updaterStatusJson.getLong("lastIncrementalCheckTimestamp");
            lastFullCheckDuration = updaterStatusJson.getInt("lastFullCheckDuration");
            lastIncrementalCheckDuration = updaterStatusJson.getInt("lastIncrementalCheckDuration");

            latestUpdatesEntries = new ArrayList<>();
            for (Object o : updaterStatusJson.getJSONArray("latestUpdatesEntries")) {
                latestUpdatesEntries.add(new LatestUpdatesEntry((JSONObject) o));
            }

            request.setAttribute("modCount", updaterStatusJson.getInt("modCount"));
        }

        ZonedDateTime lastUpdateTime = Instant.ofEpochMilli(Math.max(lastFullCheckTimestamp, lastIncrementalCheckTimestamp))
                .atZone(ZoneId.of("UTC"));

        if (lastUpdateTime.isAfter(ZonedDateTime.now().minusMinutes(30))) {
            // consider the service to be up if the latest successful update was less than 30 minutes ago.
            request.setAttribute("up", true);
        }

        fillLastCheckAttributes(request, lastFullCheckTimestamp, lastFullCheckDuration, "Full");
        fillLastCheckAttributes(request, lastIncrementalCheckTimestamp, lastIncrementalCheckDuration, "Incremental");
        fillLastCheckAttributes(request, Math.max(lastIncrementalCheckTimestamp, lastFullCheckTimestamp), 1, "");

        if ("/celeste/update-checker-status.json".equals(request.getRequestURI())) {
            updaterStatusJson.put("up", request.getAttribute("up"));
            response.setHeader("Content-Type", "application/json");
            updaterStatusJson.write(response.getWriter());
        } else {
            if (isWidget) {
                request.getRequestDispatcher("/WEB-INF/update-checker-status-widget.jsp").forward(request, response);
            } else {
                request.setAttribute("latestUpdates", latestUpdatesEntries);
                PageRenderer.render(request, response, "update-checker-status", "Everest Update Checker status page",
                        "Check the status of the Everest Update Checker here.", 60);
            }
        }
    }

    private void fillLastCheckAttributes(HttpServletRequest request, long lastCheckTimestamp, int lastCheckDuration, String name) {
        if (lastCheckTimestamp > 0) {
            ZonedDateTime timeUtc = Instant.ofEpochMilli(lastCheckTimestamp).atZone(ZoneId.of("UTC"));

            if (lastCheckDuration > 0) {
                // pass the latest check timestamp and duration to the webpage
                Pair<String, String> date = formatDate(timeUtc);
                request.setAttribute("last" + name + "CheckTimestamp", timeUtc.toEpochSecond());
                request.setAttribute("last" + name + "CheckAt", date.getLeft());
                request.setAttribute("last" + name + "CheckTimeAgo", date.getRight());
                request.setAttribute("last" + name + "CheckDuration", lastCheckDuration / 1000.0);
            }
        }
    }

    private Pair<String, String> formatDate(ZonedDateTime date) {
        String formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).format(date);
        long minutes = date.until(ZonedDateTime.now(), ChronoUnit.MINUTES);
        if (minutes == 0) {
            return Pair.of(formattedDate, "less than a minute ago");
        } else if (minutes == 1) {
            return Pair.of(formattedDate, "1 minute ago");
        } else if (minutes < 60) {
            return Pair.of(formattedDate, minutes + " minutes ago");
        } else if (minutes < 120) {
            return Pair.of(formattedDate, "1 hour ago");
        } else if (minutes < 24 * 60) {
            return Pair.of(formattedDate, (minutes / 60) + " hours ago");
        } else if (minutes < 24 * 60 * 2) {
            return Pair.of(formattedDate, "1 day ago");
        } else {
            return Pair.of(formattedDate, (minutes / (60 * 24)) + " days ago");
        }
    }
}
