package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Servlet allowing to subscribe to the #celeste_news_network channel from Celestecord.
 */
@WebServlet(name = "CelesteNewsNetworkSubscriptionService", urlPatterns = {"/celeste/news-network-subscription"})
@MultipartConfig
public class CelesteNewsNetworkSubscriptionService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteNewsNetworkSubscriptionService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("bad_request", false);
        request.setAttribute("bad_webhook", false);
        request.setAttribute("not_registered", false);
        request.setAttribute("already_registered", false);
        request.setAttribute("subscribe_success", false);
        request.setAttribute("unsubscribe_success", false);

        request.setAttribute("sub_count", countSubscribers());
        PageRenderer.render(request, response, "celeste-news-network-subscription", "#celeste_news_network Subscription Service",
                "If you want the #celeste_news_network channel from Mt. Celeste Climbing Association on your server, register your Discord webhook URL on this page!");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("bad_request", false);
        request.setAttribute("bad_webhook", false);
        request.setAttribute("not_registered", false);
        request.setAttribute("already_registered", false);
        request.setAttribute("subscribe_success", false);
        request.setAttribute("unsubscribe_success", false);

        String webhook = request.getParameter("url");
        String action = request.getParameter("action");

        if (webhook == null || action == null) {
            log.warn("Missing parameter!");
            request.setAttribute("bad_request", true);

        } else if (!webhook.matches("^https://discord\\.com/api/webhooks/[0-9]+/[A-Za-z0-9-_]+$")) {
            log.warn("Webhook does not follow regex: " + webhook);
            request.setAttribute("bad_webhook", true);

        } else {
            Path subscriberDatabase = Paths.get("/shared/celeste/celeste-news-network-subscribers.json");

            List<String> webhookUrls;
            try (InputStream is = Files.newInputStream(subscriberDatabase)) {
                webhookUrls = new JSONArray(IOUtils.toString(is, UTF_8)).toList()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            boolean save = false;

            if (action.equals("Subscribe")) {
                if (webhookUrls.contains(webhook)) {
                    log.warn("Webhook already registered: {}", webhook);
                    request.setAttribute("already_registered", true);

                } else if (isWebhookWorking(webhook)) {
                    log.info("New webhook registered: {}", webhook);
                    webhookUrls.add(webhook);
                    save = true;
                    request.setAttribute("subscribe_success", true);

                } else {
                    log.warn("Webhook does not work: {}", webhook);
                    request.setAttribute("bad_webhook", true);
                }
            } else {
                if (webhookUrls.contains(webhook)) {
                    log.info("Webhook unregistered: {}", webhook);
                    webhookUrls.remove(webhook);
                    save = true;
                    request.setAttribute("unsubscribe_success", true);

                } else {
                    log.warn("Webhook is not registered: {}", webhook);
                    request.setAttribute("not_registered", true);
                }
            }

            if (save) {
                Files.writeString(subscriberDatabase, new JSONArray(webhookUrls).toString());
            }
        }

        request.setAttribute("sub_count", countSubscribers());
        PageRenderer.render(request, response, "celeste-news-network-subscription", "#celeste_news_network Subscription Service",
                "If you want the #celeste_news_network channel from Mt. Celeste Climbing Association on your server, register your Discord webhook URL on this page!");
    }

    /**
     * Checks if the webhook is working, by sending "This webhook was registered on the #celeste_news_network subscription service!"
     * to it. This doubles as a notification in the server.
     *
     * @param webhookUrl The URL of the webhook to test
     * @return Whether the call succeeded or not
     */
    private boolean isWebhookWorking(String webhookUrl) {
        try {
            HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(webhookUrl);

            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            connection.connect();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write("{\"avatar_url\": \"https://cdn.discordapp.com/embed/avatars/0.png\", \"username\": \"Subscription Service\"," +
                    " \"content\": \"This webhook was registered on the #celeste_news_network subscription service!\"}");
            writer.close();

            return connection.getResponseCode() == 204 || connection.getResponseCode() == 200;
        } catch (IOException e) {
            // well, the webhook is definitely not working. :p
            log.error("I/O error while calling webhook", e);
            return false;
        }
    }

    private int countSubscribers() throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/celeste-news-network-subscribers.json"))) {
            return new JSONArray(IOUtils.toString(is, UTF_8)).length();
        }
    }
}
