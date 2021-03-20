package com.max480.randomstuff.gae;

import com.google.api.core.ApiFuture;
import com.google.api.gax.paging.AsyncPage;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;

@WebServlet(name = "UpdateCheckerStatus", loadOnStartup = 7, urlPatterns = {"/celeste/update-checker-status"})
@MultipartConfig
public class UpdateCheckerStatusService extends HttpServlet {
    private static Logging logging = LoggingOptions.getDefaultInstance().getService();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // the service is down until proven otherwise.
        request.setAttribute("up", false);

        // check logs for the last successful update (limit 24 hours)
        final ApiFuture<AsyncPage<LogEntry>> lastCheck = logging.listLogEntriesAsync(
                Logging.EntryListOption.sortOrder(Logging.SortingField.TIMESTAMP, Logging.SortingOrder.DESCENDING),
                Logging.EntryListOption.filter("jsonPayload.message =~ \"^=== Ended searching for updates.\""),
                Logging.EntryListOption.pageSize(1)
        );
        // check logs for last update with updates found (limit 7 days)
        final ApiFuture<AsyncPage<LogEntry>> lastCheckWithUpdates = logging.listLogEntriesAsync(
                Logging.EntryListOption.sortOrder(Logging.SortingField.TIMESTAMP, Logging.SortingOrder.DESCENDING),
                Logging.EntryListOption.filter("(" +
                        "jsonPayload.message =~ \"^=> Saved new information to database:\" " +
                        "OR jsonPayload.message =~ \"was deleted from the database$\" " +
                        "OR jsonPayload.message =~ \"Adding to the excluded files list.$\" " +
                        "OR jsonPayload.message =~ \"Adding to the no yaml files list.$\")" +
                        " AND timestamp >= \"" + ZonedDateTime.now().minusDays(7).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\""),
                Logging.EntryListOption.pageSize(1)
        );

        // check mod count by just downloading the mod database and counting objects in it.
        try (InputStream is = new URL("https://max480-random-stuff.appspot.com/celeste/everest_update.yaml").openStream()) {
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
                    request.setAttribute("lastUpdated", formatDate(timeUtc));
                    request.setAttribute("duration", new DecimalFormat("0.0").format(timeMs / 1000.0));
                }
            }

            for (LogEntry entry : lastCheckWithUpdates.get().getValues()) {
                ZonedDateTime timeUtc = Instant.ofEpochMilli(entry.getTimestamp()).atZone(ZoneId.of("UTC"));
                request.setAttribute("lastUpdateFound", formatDate(timeUtc));
            }

            request.getRequestDispatcher("/WEB-INF/update-checker-status.jsp").forward(request, response);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    private String formatDate(ZonedDateTime date) {
        String formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).format(date);
        long minutes = date.until(ZonedDateTime.now(), ChronoUnit.MINUTES);
        if (minutes == 0) {
            return formattedDate + " (less than a minute ago)";
        } else if (minutes == 1) {
            return formattedDate + " (1 minute ago)";
        } else if (minutes < 60) {
            return formattedDate + " (" + minutes + " minutes ago)";
        } else if (minutes < 120) {
            return formattedDate + " (1 hour ago)";
        } else if (minutes < 24 * 60) {
            return formattedDate + " (" + (minutes / 60) + " hours ago)";
        } else if (minutes < 24 * 60 * 2) {
            return formattedDate + " (1 day ago)";
        } else {
            return formattedDate + " (" + (minutes / (60 * 24)) + " days ago)";
        }
    }
}
