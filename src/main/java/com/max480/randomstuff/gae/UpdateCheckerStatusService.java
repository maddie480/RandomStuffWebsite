package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        public boolean isAddition;
        public String name;
        public String version;
        public String url;
        public String date;
        public long timestamp;

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

        long lastCheckTimestamp;
        int lastCheckDuration;
        List<LatestUpdatesEntry> latestUpdatesEntries;
        JSONObject updaterStatusJson;

        // retrieve the update checker status from Cloud Storage!
        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("update_checker_status.json")) {
            updaterStatusJson = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));

            lastCheckTimestamp = updaterStatusJson.getLong("lastFullCheckTimestamp");
            lastCheckDuration = updaterStatusJson.getInt("lastCheckDuration");
            latestUpdatesEntries = new ArrayList<>();
            for (Object o : updaterStatusJson.getJSONArray("latestUpdatesEntries")) {
                latestUpdatesEntries.add(new LatestUpdatesEntry((JSONObject) o));
            }

            request.setAttribute("modCount", updaterStatusJson.getInt("modCount"));
        }

        if (lastCheckTimestamp > 0) {
            ZonedDateTime timeUtc = Instant.ofEpochMilli(lastCheckTimestamp).atZone(ZoneId.of("UTC"));

            if (timeUtc.isAfter(ZonedDateTime.now().minusMinutes(30))) {
                // consider the service to be up if the latest successful update was less than 30 minutes ago.
                request.setAttribute("up", true);
            }

            if (lastCheckDuration > 0) {
                // pass the latest check timestamp and duration to the webpage
                Pair<String, String> date = formatDate(timeUtc);
                request.setAttribute("lastUpdatedTimestamp", timeUtc.toEpochSecond());
                request.setAttribute("lastUpdatedAt", date.getLeft());
                request.setAttribute("lastUpdatedAgo", date.getRight());
                request.setAttribute("duration", lastCheckDuration / 1000.0);
            }
        }

        if ("/celeste/update-checker-status.json".equals(request.getRequestURI())) {
            updaterStatusJson.put("up", request.getAttribute("up"));
            response.setHeader("Content-Type", "application/json");
            response.getWriter().write(updaterStatusJson.toString());
        } else {
            if (!isWidget) {
                request.setAttribute("latestUpdates", latestUpdatesEntries);
            }

            request.getRequestDispatcher(isWidget ? "/WEB-INF/update-checker-status-widget.jsp" : "/WEB-INF/update-checker-status.jsp")
                    .forward(request, response);
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
