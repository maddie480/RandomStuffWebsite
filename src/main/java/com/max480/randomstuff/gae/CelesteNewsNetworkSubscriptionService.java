package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Servlet allowing to subscribe to the #celeste_news_network channel from Celestecord.
 */
@WebServlet(name = "CelesteNewsNetworkSubscriptionService", urlPatterns = {"/celeste/news-network-subscription"})
@MultipartConfig
public class CelesteNewsNetworkSubscriptionService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("CelesteNewsNetworkSubscriptionService");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("bad_request", false);
        request.setAttribute("bad_webhook", false);
        request.setAttribute("not_registered", false);
        request.setAttribute("already_registered", false);
        request.setAttribute("subscribe_success", false);
        request.setAttribute("unsubscribe_success", false);

        request.setAttribute("sub_count", countSubscribers());
        request.getRequestDispatcher("/WEB-INF/celeste-news-network-subscription.jsp").forward(request, response);
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
            logger.warning("Missing parameter!");
            request.setAttribute("bad_request", true);

        } else if (!webhook.matches("^https://discord\\.com/api/webhooks/[0-9]+/[A-Za-z0-9-_]+$")) {
            logger.warning("Webhook does not follow regex: " + webhook);
            request.setAttribute("bad_webhook", true);

        } else {
            List<String> webhookUrls;
            try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("celeste_news_network_subscribers.json")) {
                webhookUrls = new JSONArray(IOUtils.toString(is, UTF_8)).toList()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            boolean save = false;

            if (action.equals("Subscribe")) {
                if (webhookUrls.contains(webhook)) {
                    logger.warning("Webhook already registered: " + webhook);
                    request.setAttribute("already_registered", true);

                } else if (isWebhookWorking(webhook)) {
                    logger.info("New webhook registered: " + webhook);
                    webhookUrls.add(webhook);
                    save = true;
                    request.setAttribute("subscribe_success", true);

                } else {
                    logger.warning("Webhook does not work: " + webhook);
                    request.setAttribute("bad_webhook", true);
                }
            } else {
                if (webhookUrls.contains(webhook)) {
                    logger.info("Webhook unregistered: " + webhook);
                    webhookUrls.remove(webhook);
                    save = true;
                    request.setAttribute("unsubscribe_success", true);

                } else {
                    logger.warning("Webhook is not registered: " + webhook);
                    request.setAttribute("not_registered", true);
                }
            }

            if (save) {
                CloudStorageUtils.sendBytesToCloudStorage("celeste_news_network_subscribers.json", "application/json",
                        new JSONArray(webhookUrls).toString().getBytes(UTF_8));
            }
        }

        request.setAttribute("sub_count", countSubscribers());
        request.getRequestDispatcher("/WEB-INF/celeste-news-network-subscription.jsp").forward(request, response);
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
            HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0");

            connection.connect();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write("{\"avatar_url\": \"https://cdn.discordapp.com/embed/avatars/0.png\", \"username\": \"Subscription Service\"," +
                    " \"content\": \"This webhook was registered on the #celeste_news_network subscription service!\"}");
            writer.close();

            return connection.getResponseCode() == 204 || connection.getResponseCode() == 200;
        } catch (IOException e) {
            // well, the webhook is definitely not working. :p
            logger.severe("I/O error while calling webhook: " + e);
            return false;
        }
    }

    private int countSubscribers() throws IOException {
        try (InputStream is = CloudStorageUtils.getCloudStorageInputStream("celeste_news_network_subscribers.json")) {
            return new JSONArray(IOUtils.toString(is, UTF_8)).length();
        }
    }
}
