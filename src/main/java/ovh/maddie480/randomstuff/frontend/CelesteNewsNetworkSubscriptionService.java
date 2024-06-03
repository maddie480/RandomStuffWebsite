package ovh.maddie480.randomstuff.frontend;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet allowing to subscribe to the #celeste_news_network channel from Celestecord.
 */
@WebServlet(name = "CelesteNewsNetworkSubscriptionService", urlPatterns = {"/celeste/news-network-subscription"})
@MultipartConfig
public class CelesteNewsNetworkSubscriptionService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteNewsNetworkSubscriptionService.class);

    private record Subscriber(String webhook, List<String> channels) {
    }

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

        List<String> channels = new ArrayList<>(3);
        for (String channel : Arrays.asList("twitter", "mastodon", "olympus")) {
            if (request.getParameter(channel) != null) {
                channels.add(channel);
            }
        }

        if (channels.isEmpty()) {
            // Subscribing to nothing actually means unsubscribing :thonk:
            // Most users should not run into this since the Subscribe button is greyed out in that case.
            action = "Unsubscribe";
        }

        if (webhook == null || action == null) {
            log.warn("Missing parameter!");
            request.setAttribute("bad_request", true);

        } else if (!webhook.matches("^https://discord\\.com/api/webhooks/[0-9]+/[A-Za-z0-9-_]+$")) {
            log.warn("Webhook does not follow regex: {}", webhook);
            request.setAttribute("bad_webhook", true);

        } else {
            Path subscriberDatabase = Paths.get("/shared/celeste/celeste-news-network-subscribers.json");

            List<Subscriber> subscribers;
            try (BufferedReader br = Files.newBufferedReader(subscriberDatabase)) {
                subscribers = new JSONArray(new JSONTokener(br)).toList()
                        .stream()
                        .map(object -> {
                            Map<String, Object> subscriberRaw = (Map<String, Object>) object;
                            return new Subscriber((String) subscriberRaw.get("webhook"), (List<String>) subscriberRaw.get("channels"));
                        })
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            boolean save = false;

            StringBuilder message = new StringBuilder(":white_check_mark: **This webhook was registered on the #celeste_news_network subscription service!**\\nIt will now receive news from ");
            for (int i = 0; i < channels.size(); i++) {
                if (i != 0) {
                    if (i == channels.size() - 1) message.append(" and ");
                    else message.append(", ");
                }
                message.append(StringUtils.capitalize(channels.get(i)));
            }
            message.append(".");

            Subscriber existingSubscriber = subscribers.stream()
                    .filter(subscriber -> subscriber.webhook.equals(webhook))
                    .findFirst().orElse(null);

            if (action.equals("Subscribe")) {
                // if a subscriber with the same URL already exists, remove it so that we replace it with the new subscription options
                if (existingSubscriber != null) {
                    log.warn("Webhook already registered: {}", webhook);
                    request.setAttribute("already_registered", true);
                    subscribers.remove(existingSubscriber);
                }

                if (isWebhookWorking(webhook, message.toString())) {
                    log.info("New webhook registered: {}, channels: {}", webhook, channels);
                    subscribers.add(new Subscriber(webhook, channels));
                    save = true;
                    request.setAttribute("subscribe_success", true);

                } else {
                    log.warn("Webhook does not work: {}", webhook);
                    request.setAttribute("bad_webhook", true);
                }
            } else {
                if (subscribers.contains(existingSubscriber)) {
                    log.info("Webhook unregistered: {}", webhook);
                    subscribers.remove(existingSubscriber);
                    save = true;
                    request.setAttribute("unsubscribe_success", true);

                } else {
                    log.warn("Webhook is not registered: {}", webhook);
                    request.setAttribute("not_registered", true);
                }
            }

            if (save) {
                JSONArray data = new JSONArray(subscribers.stream()
                        .map(subscriber -> ImmutableMap.of(
                                "webhook", subscriber.webhook,
                                "channels", subscriber.channels
                        ))
                        .toList());

                try (BufferedWriter bw = Files.newBufferedWriter(subscriberDatabase)) {
                    data.write(bw);
                }
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
     * @param message    The message to send (must be safe to be included as is in a JSON string )
     * @return Whether the call succeeded or not
     */
    private boolean isWebhookWorking(String webhookUrl, String message) {
        try {
            HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(webhookUrl);

            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            connection.connect();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write("{\"avatar_url\": \"https://cdn.discordapp.com/embed/avatars/0.png\", \"username\": \"Subscription Service\"," +
                    " \"content\": \"" + message + "\"}");
            writer.close();

            return connection.getResponseCode() == 204 || connection.getResponseCode() == 200;
        } catch (IOException e) {
            // well, the webhook is definitely not working. :p
            log.error("I/O error while calling webhook", e);
            return false;
        }
    }

    private int countSubscribers() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get("/shared/celeste/celeste-news-network-subscribers.json"))) {
            return new JSONArray(new JSONTokener(br)).length();
        }
    }
}
