package com.max480.randomstuff.gae;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

public class WebhookExecutor {
    private static final Logger logger = Logger.getLogger("WebhookExecutor");

    private static ZonedDateTime retryAfter = null;

    public static void executeWebhook(String webhookUrl, String body) throws IOException, InterruptedException {
        if (retryAfter != null) {
            long waitFor = ZonedDateTime.now().until(retryAfter, ChronoUnit.MILLIS);
            if (waitFor > 0) {
                logger.warning("Waiting " + waitFor + " ms before request because of rate limits.");
                Thread.sleep(waitFor);
            }
        }
        retryAfter = null;

        logger.info("Sending message [" + body + "] to webhook [" + webhookUrl + "]");

        HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();

        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0");

        connection.connect();

        JSONObject request = new JSONObject();
        request.put("content", body);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        writer.write(request.toString());
        writer.close();

        if (connection.getResponseCode() != 204) {
            throw new IOException("Non-204 return code: " + connection.getResponseCode());
        }

        logger.fine("Message sent!");
        if ("0".equals(connection.getHeaderField("X-RateLimit-Remaining"))) {
            try {
                retryAfter = ZonedDateTime.now().plusSeconds(Integer.parseInt(connection.getHeaderField("X-RateLimit-Reset-After")) + 1);
                logger.warning("We hit rate limit! We will wait until " + retryAfter + " before next request.");
            } catch (Exception e) {
                retryAfter = ZonedDateTime.now().plusSeconds(15);
                logger.warning("We hit rate limit! We will wait until " + retryAfter + " before next request. (parsing X-RateLimit-Reset-After failed)");
            }
        }
    }
}

