package com.max480.randomstuff.gae.discord.bananabot;

import com.max480.randomstuff.gae.CelesteModSearchService;
import com.max480.randomstuff.gae.ConnectionUtils;
import com.max480.randomstuff.gae.SecretConstants;
import com.max480.randomstuff.gae.discord.DiscordProtocolHandler;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This is the API that makes the BananaBot run.
 */
@WebServlet(name = "BananaBot", urlPatterns = {"/discord/bananabot"})
public class InteractionManager extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(InteractionManager.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject data = DiscordProtocolHandler.validateRequest(req, resp, SecretConstants.BANANABOT_PUBLIC_KEY);
        if (data == null) return;

        log.debug("Guild {} used the BananaBot!", data.getString("guild_id"));

        String locale = data.getString("locale");

        try {
            if (data.getInt("type") == 3) {
                JSONObject response = new JSONObject();
                response.put("type", 7); // edit the original response

                JSONObject responseData = new JSONObject();

                if (data.getJSONObject("data").getInt("component_type") == 2) {
                    // used a button
                    responseData.put("content", localizeMessage(locale, "Your link was published!", "Ton lien a été publié !"));
                    responseData.put("components", new JSONArray());

                    String linkToPost = data.getJSONObject("message").getString("content");
                    JSONObject followupMessageData = new JSONObject();
                    followupMessageData.put("content", linkToPost + "\n(shared by <@" + data.getJSONObject("member").getJSONObject("user").getString("id") + ">)");
                    followupMessageData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));

                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);

                            String webhookUrl = "https://discord.com/api/v10/webhooks/" + data.getString("application_id") + "/" + data.getString("token");
                            log.debug("Sending followup message request to {}: {}", webhookUrl, followupMessageData.toString(2));

                            HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(webhookUrl);
                            connection.setRequestProperty("Content-Type", "application/json");
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            try (OutputStream os = connection.getOutputStream()) {
                                IOUtils.write(followupMessageData.toString(), os, StandardCharsets.UTF_8);
                            }
                            try (InputStream is = ConnectionUtils.connectionToInputStream(connection)) {
                                JSONObject discordResponse = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                                log.debug("Got response: {}", discordResponse.toString(2));
                            }
                        } catch (IOException | InterruptedException e) {
                            log.error("An unexpected error occurred while sending followup message!", e);
                        }
                    }).start();
                } else {
                    // used a combo box
                    String pickedMod = data.getJSONObject("data").getJSONArray("values").getString(0);
                    responseData.put("content", pickedMod);
                    responseData.put("flags", 1 << 6); // ephemeral
                }

                responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
                response.put("data", responseData);

                log.debug("Responding with: " + response.toString(2));
                resp.getWriter().write(response.toString());
            } else {
                // slash command invocation
                String searchTerms = data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value");
                List<Map<String, Object>> results = CelesteModSearchService.searchModsByName(searchTerms);

                if (results.isEmpty()) {
                    respondPrivately(resp, localizeMessage(locale,
                            ":x: No results! Please check your search terms.",
                            ":x: Aucun résultat ! Vérifie tes termes de recherche."));
                } else {
                    respondWithDropdown(resp, locale, results);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred!", e);
            respondPrivately(resp, localizeMessage(locale,
                    ":x: An unexpected error occurred. Ping Maddie (`maddie480#4596`) if this keeps happening!",
                    ":x: Une erreur inattendue est survenue. Ping Maddie (`maddie480#4596`) si ça continue à arriver !"));
        }
    }

    private static void respondWithDropdown(HttpServletResponse resp, String locale, List<Map<String, Object>> results) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("content", results.get(0).get("PageURL"));
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
        responseData.put("flags", 1 << 6); // ephemeral
        response.put("data", responseData);

        JSONArray components = new JSONArray();
        responseData.put("components", components);

        // === dropdown action row

        JSONObject actionRow = new JSONObject();
        components.put(actionRow);

        actionRow.put("type", 1);

        JSONArray rowComponents = new JSONArray();
        actionRow.put("components", rowComponents);

        JSONObject selectMenu = new JSONObject();
        rowComponents.put(selectMenu);

        selectMenu.put("type", 3);
        selectMenu.put("custom_id", "search-results");

        JSONArray options = new JSONArray();
        selectMenu.put("options", options);

        for (Map<String, Object> result : results) {
            JSONObject object = new JSONObject();
            object.put("label", result.get("Name"));
            object.put("value", result.get("PageURL"));
            options.put(object);
        }

        // === button action row

        actionRow = new JSONObject();
        components.put(actionRow);

        actionRow.put("type", 1);

        rowComponents = new JSONArray();
        actionRow.put("components", rowComponents);

        JSONObject button = new JSONObject();
        rowComponents.put(button);

        button.put("type", 2);
        button.put("style", 1);
        button.put("label", localizeMessage(locale, "Post publicly", "Poster publiquement"));
        button.put("custom_id", "repost");

        JSONObject emojiObject = new JSONObject("{\"id\": null}");
        button.put("emoji", emojiObject);
        emojiObject.put("name", "\uD83D\uDCE4"); // outbox tray

        log.debug("Responding with: " + response.toString(2));
        resp.getWriter().write(response.toString());
    }

    /**
     * Responds privately to a slash command, in response to the HTTP request.
     */
    private static void respondPrivately(HttpServletResponse responseStream, String message) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 4); // response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("content", message);
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));
        responseData.put("flags", 1 << 6); // ephemeral
        response.put("data", responseData);

        log.debug("Responding with: " + response.toString(2));
        responseStream.getWriter().write(response.toString());
    }

    private static String localizeMessage(String locale, String english, String french) {
        if ("fr".equals(locale)) {
            return french;
        }

        return english;
    }
}
