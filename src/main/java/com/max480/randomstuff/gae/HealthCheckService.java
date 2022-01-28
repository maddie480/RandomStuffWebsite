package com.max480.randomstuff.gae;

import com.google.api.gax.paging.Page;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * A simple service that just responds with "yes the app exists", for use with availability checks.
 */
@WebServlet(name = "HealthCheckService", urlPatterns = {"/healthcheck", "/bot-healthcheck"})
public class HealthCheckService extends HttpServlet {
    private final Logger logger = Logger.getLogger("HealthCheckService");
    private static Logging logging = LoggingOptions.getDefaultInstance().getService();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/healthcheck")) {
            // server healthcheck
            response.setHeader("Content-Type", "text/plain");
            response.getWriter().write("The service is up and running!");
        } else if (request.getRequestURI().equals("/bot-healthcheck")) {
            response.setHeader("Content-Type", "text/plain");

            // was the bot CONNECTED less than 3 minutes ago?
            final Page<LogEntry> logEntry = logging.listLogEntries(
                    Logging.EntryListOption.sortOrder(Logging.SortingField.TIMESTAMP, Logging.SortingOrder.DESCENDING),
                    Logging.EntryListOption.filter(
                            "jsonPayload.message =\"Bot status is CONNECTED\" " +
                                    " AND timestamp >= \"" + ZonedDateTime.now().minusMinutes(isExtendedDelay() ? 15 : 2).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\""),
                    Logging.EntryListOption.pageSize(1)
            );

            if (logEntry.getValues().iterator().hasNext()) {
                logger.fine("Last OK log is from " + logEntry.getValues().iterator().next().getTimestamp());
                response.getWriter().write("OK");
            } else {
                response.getWriter().write("KO");
            }
        } else {
            logger.warning("Route not found");
            response.setStatus(404);
        }
    }

    private boolean isExtendedDelay() {
        // The bot stops reporting its status during the midnight checks,
        // and at 8am on Sundays due to a backup scheduled task.
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        return (now.getHour() == 0 && now.getMinute() < 15)
            || (now.getDayOfWeek() == DayOfWeek.SUNDAY && now.getHour() == 8 && now.getMinute() < 15);
    }
}
