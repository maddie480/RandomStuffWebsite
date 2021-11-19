package com.max480.randomstuff.gae;

import com.google.api.core.ApiFuture;
import com.google.api.gax.paging.AsyncPage;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import org.apache.commons.lang3.tuple.Pair;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.max480.randomstuff.gae.ConnectionUtils.openStreamWithTimeout;

/**
 * A status page for the Update Checker, that determines the status by reading the Google Cloud Logging logs
 * of the backend server.
 */
@WebServlet(name = "UpdateCheckerStatus", urlPatterns = {"/celeste/update-checker-status"})
@MultipartConfig
public class UpdateCheckerStatusService extends HttpServlet {
    private static final Logging logging = LoggingOptions.getDefaultInstance().toBuilder().setProjectId("max480-random-stuff").build().getService();

    private static final Pattern INFORMATION_EXTRACTOR = Pattern.compile(".* Mod\\{name='(.+)', version='([0-9.]+)',.*");

    public static class LatestUpdatesEntry {
        public boolean isAddition;
        public String name;
        public String version;
        public String date;
        public long timestamp;

        public LatestUpdatesEntry(boolean isAddition, String name, String version, String date, long timestamp) {
            this.isAddition = isAddition;
            this.name = name;
            this.version = version;
            this.date = date;
            this.timestamp = timestamp;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final boolean isWidget = "true".equals(request.getParameter("widget"));

        // the service is down until proven otherwise.
        request.setAttribute("up", false);

        // check logs for the last successful update (limit 24 hours)
        final ApiFuture<AsyncPage<LogEntry>> lastCheck = logging.listLogEntriesAsync(
                Logging.EntryListOption.sortOrder(Logging.SortingField.TIMESTAMP, Logging.SortingOrder.DESCENDING),
                Logging.EntryListOption.filter("jsonPayload.message =~ \"^=== Ended searching for updates.\""),
                Logging.EntryListOption.pageSize(1)
        );

        // check logs for latest database changes (limit 7 days)
        ApiFuture<AsyncPage<LogEntry>> latestUpdates = null;

        if (!isWidget) {
            latestUpdates = logging.listLogEntriesAsync(
                    Logging.EntryListOption.sortOrder(Logging.SortingField.TIMESTAMP, Logging.SortingOrder.DESCENDING),
                    Logging.EntryListOption.filter("(" +
                            "jsonPayload.message =~ \"^=> Saved new information to database:\" " +
                            "OR jsonPayload.message =~ \"^Mod .* was deleted from the database$\")" +
                            " AND timestamp >= \"" + ZonedDateTime.now().minusDays(7).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\""),
                    Logging.EntryListOption.pageSize(5)
            );
        }

        // check mod count by just downloading the mod database and counting objects in it.
        try (InputStream is = openStreamWithTimeout(new URL("https://max480-random-stuff.appspot.com/celeste/everest_update.yaml"))) {
            Map<Object, Object> mods = new Yaml().load(is);
            request.setAttribute("modCount", mods.size());
        }

        try {
            for (LogEntry entry : lastCheck.get().getValues()) {
                ZonedDateTime timeUtc = Instant.ofEpochMilli(entry.getTimestamp()).atZone(ZoneId.of("UTC"));

                if (timeUtc.isAfter(ZonedDateTime.now().minusMinutes(30))) {
                    // consider the service to be up if the latest successful update was less than 30 minutes ago.
                    request.setAttribute("up", true);
                }

                // extract the duration
                String logContent = entry.<Payload.JsonPayload>getPayload().getDataAsMap().get("message").toString();
                if (logContent.endsWith(" ms.")) {
                    logContent = logContent.substring(0, logContent.length() - 4);
                    logContent = logContent.substring(logContent.lastIndexOf(" ") + 1);
                    int timeMs = Integer.parseInt(logContent);

                    // pass it to the webpage
                    Pair<String, String> date = formatDate(timeUtc);
                    request.setAttribute("lastUpdatedTimestamp", timeUtc.toEpochSecond());
                    request.setAttribute("lastUpdatedAt", date.getLeft());
                    request.setAttribute("lastUpdatedAgo", date.getRight());
                    request.setAttribute("duration", new DecimalFormat("0.0").format(timeMs / 1000.0));
                }
            }

            if (!isWidget) {
                List<LatestUpdatesEntry> latestUpdatesEntries = new ArrayList<>();

                for (LogEntry logEntry : latestUpdates.get().getValues()) {
                    String message = (String) logEntry.<Payload.JsonPayload>getPayload().getDataAsMap().get("message");

                    final Matcher matcher = INFORMATION_EXTRACTOR.matcher(message);
                    if (matcher.matches()) {
                        latestUpdatesEntries.add(new LatestUpdatesEntry(
                                message.startsWith("=> Saved new information to database:"),
                                matcher.group(1),
                                matcher.group(2),
                                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(logEntry.getInstantTimestamp().atZone(ZoneId.of("UTC"))),
                                logEntry.getInstantTimestamp().toEpochMilli() / 1000L
                        ));
                    }
                }

                request.setAttribute("latestUpdates", latestUpdatesEntries);
            }

            request.getRequestDispatcher(isWidget ? "/WEB-INF/update-checker-status-widget.jsp" : "/WEB-INF/update-checker-status.jsp")
                    .forward(request, response);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
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
