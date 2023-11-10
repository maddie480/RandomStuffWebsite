package ovh.maddie480.randomstuff.frontend.discord.newspublisher;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.io.function.IORunnable;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.frontend.ConnectionUtils;
import ovh.maddie480.randomstuff.frontend.SecretConstants;
import ovh.maddie480.randomstuff.frontend.discord.DiscordProtocolHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * This is the API that makes the Olympus News manager run.
 */
@WebServlet(name = "OlympusNewsManager", urlPatterns = {"/discord/olympus-news-manager"}, loadOnStartup = 7)
public class InteractionManager extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(InteractionManager.class);

    private static final NewsAuthor NEWS_COMMITTER = new NewsAuthor(52103563, "maddie480");
    private static final Map<Long, NewsAuthor> NEWS_AUTHORS = ImmutableMap.of(
            354341658352943115L, NEWS_COMMITTER,
            191579321901514753L, new NewsAuthor(24738390, "Nyan-Games"),
            633114231935336489L, new NewsAuthor(67283043, "campbell-godfrey"),
            444598188452741180L, new NewsAuthor(127329763, "cellularAutomaton")
    );

    @Override
    public void init() {
        try {
            GitOperator.sshInit();
            GitOperator.init();
            log.info("Done initializing the git repository for the news");
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject data = DiscordProtocolHandler.validateRequest(req, resp, SecretConstants.OLYMPUS_NEWS_MANAGER_PUBLIC_KEY);
        if (data == null) return;

        log.debug("Guild {} used the Olympus News Manager!", data.getString("guild_id"));

        String interactionToken = data.getString("token");
        long memberId = Long.parseLong(data.getJSONObject("member").getJSONObject("user").getString("id"));

        try {
            if (data.getInt("type") == 4) {
                // autocomplete
                commandAutocomplete(data, resp);

            } else if (data.getInt("type") == 5) {
                // form submit
                String interactionId = data.getJSONObject("data").getString("custom_id");
                JSONArray contents = data.getJSONObject("data").getJSONArray("components");
                OlympusNews news = buildFromInteraction(contents);

                if (interactionId.equals("new")) {
                    runDeferred(interactionToken, resp, () -> create(news, memberId));
                } else {
                    searchSlugAndRun(interactionId, oldNews -> runDeferred(interactionToken, resp, () -> update(interactionId, oldNews.image(), news, memberId)), resp);
                }
            } else {
                // slash command invocation
                String commandName = data.getJSONObject("data").getString("name");

                switch (commandName) {
                    case "post-news" -> sendEditOrCreateForm(resp, null);
                    case "edit-news" ->
                            searchSlugAndRun(data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"),
                                    news -> sendEditOrCreateForm(resp, news), resp);
                    case "archive-news" ->
                            archive(resp, interactionToken, data.getJSONObject("data").getJSONArray("options").getJSONObject(0).getString("value"), memberId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("An unexpected error occurred!", e);
            respondPrivately(resp, ":x: An unexpected error occurred. Reach out to Maddie if this keeps happening!");
        }
    }

    public static void create(OlympusNews news, long memberId) throws IOException {
        byte[] image = null;
        if (news.image() != null) {
            try (InputStream is = ConnectionUtils.openStreamWithTimeout(news.image())) {
                image = IOUtils.toByteArray(is);
            }
        }

        GitOperator.init();
        GitOperator.createOlympusNews(news, image);

        List<OlympusNews> list = GitOperator.listOlympusNews();
        while (list.size() > 10) {
            GitOperator.archiveOlympusNews(list.get(0));
            list = GitOperator.listOlympusNews();
        }

        GitOperator.commitChanges(NEWS_AUTHORS.get(memberId), NEWS_COMMITTER);
    }

    public static void update(String customId, String image, OlympusNews news, long memberId) throws IOException {
        news = new OlympusNews(customId, news.title(), image, news.link(), news.shortDescription(), news.longDescription());
        GitOperator.init();
        GitOperator.updateOlympusNews(news);
        GitOperator.commitChanges(NEWS_AUTHORS.get(memberId), NEWS_COMMITTER);
    }

    public static void archive(HttpServletResponse response, String interactionToken, String slug, long memberId) throws IOException {
        searchSlugAndRun(slug, news -> runDeferred(interactionToken, response, () -> {
            GitOperator.init();
            GitOperator.archiveOlympusNews(news);
            GitOperator.commitChanges(NEWS_AUTHORS.get(memberId), NEWS_COMMITTER);
        }), response);
    }

    private void commandAutocomplete(JSONObject interaction, HttpServletResponse resp) throws IOException {
        String partialNewsTitle = "";
        for (Object o : interaction.getJSONObject("data").getJSONArray("options")) {
            JSONObject option = (JSONObject) o;
            if ("title".equals(option.getString("name"))) {
                partialNewsTitle = option.getString("value");
            }
        }

        JSONObject response = new JSONObject();
        response.put("type", 8); // autocomplete result

        JSONObject responseData = new JSONObject();
        JSONArray choices = new JSONArray();

        // if the command name isn't valid, no need to bother checking if it exists.
        final String prefix = partialNewsTitle.toLowerCase(Locale.ROOT);

        GitOperator.listOlympusNews().stream()
                .filter(news -> news.title().toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(Comparator.comparing(OlympusNews::slug).reversed())
                .limit(25)
                .map(news -> {
                    JSONObject choice = new JSONObject();
                    choice.put("name", news.title());
                    choice.put("value", news.slug());
                    return choice;
                })
                .forEach(choices::put);

        responseData.put("choices", choices);
        response.put("data", responseData);

        log.debug("Responding with: {}", response.toString(2));
        resp.getWriter().write(response.toString());
    }


    private static void searchSlugAndRun(String slug, IOConsumer<OlympusNews> run, HttpServletResponse resp) throws IOException {
        Optional<OlympusNews> news = GitOperator.listOlympusNews()
                .stream().filter(n -> n.slug().equals(slug))
                .findFirst();

        if (news.isPresent()) {
            run.accept(news.get());
        } else {
            respondPrivately(resp, ":x: The requested news entry does not exist!");
        }
    }

    private static void runDeferred(String interactionToken, HttpServletResponse response, IORunnable task) throws IOException {
        respondLater(response);

        new Thread(() -> {
            try {
                task.run();
                respondDeferred(interactionToken, ":white_check_mark: Olympus news update completed!");
            } catch (IOException e) {
                log.error("Error while running deferred process!", e);

                try {
                    // reset the local git repository in an attempt to clean up whatever happened
                    GitOperator.init();
                    respondDeferred(interactionToken, ":x: An unexpected error occurred. Reach out to Maddie if this keeps happening!");
                } catch (IOException e2) {
                    log.error("Error while handling error!", e2);
                }
            }
        }).start();
    }

    public static OlympusNews buildFromInteraction(JSONArray options) {
        OlympusNews news = new OlympusNews(null, null, null, null, null, null);

        for (Object item : options) {
            JSONObject itemObject = ((JSONObject) item).getJSONArray("components").getJSONObject(0);

            // consider that empty fields do not exist at all.
            if (itemObject.get("value") instanceof String && itemObject.getString("value").isEmpty()) {
                continue;
            }

            String value = itemObject.getString("value");
            if (value.isBlank()) continue;

            switch (itemObject.getString("custom_id")) {
                case "title" ->
                        news = new OlympusNews(null, value, news.image(), news.link(), news.shortDescription(), news.longDescription());
                case "image" ->
                        news = new OlympusNews(null, news.title(), value, news.link(), news.shortDescription(), news.longDescription());
                case "link" ->
                        news = new OlympusNews(null, news.title(), news.image(), value, news.shortDescription(), news.longDescription());
                case "short_description" ->
                        news = new OlympusNews(null, news.title(), news.image(), news.link(), value, news.longDescription());
                case "long_description" ->
                        news = new OlympusNews(null, news.title(), news.image(), news.link(), news.shortDescription(), value);
            }
        }

        return news;
    }

    /**
     * Displays the form allowing to edit or create a news entry.
     */
    private static void sendEditOrCreateForm(HttpServletResponse resp, OlympusNews news) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 9); // modal

        JSONObject responseData = new JSONObject();

        responseData.put("custom_id", news == null ? "new" : news.slug());
        responseData.put("title", news == null ? "Post a News Entry" : "Edit a News Entry");

        JSONArray componentData = new JSONArray();
        componentData.put(getComponentDataForTextInput("title", "Title",
                news == null || news.title() == null ? "" : news.title(), 100, false));
        if (news == null) {
            componentData.put(getComponentDataForTextInput("image", "Image URL", "", 4000, false));
        }
        componentData.put(getComponentDataForTextInput("link", "Link",
                news == null || news.link() == null ? "" : news.link(), 4000, false));
        componentData.put(getComponentDataForTextInput("short_description", "Short Description",
                news == null || news.shortDescription() == null ? "" : news.shortDescription(), 4000, true));
        componentData.put(getComponentDataForTextInput("long_description", "Long Description",
                news == null || news.longDescription() == null ? "" : news.longDescription(), 4000, true));
        responseData.put("components", componentData);

        response.put("data", responseData);
        log.debug("Responding with: {}", response.toString(2));
        resp.getWriter().write(response.toString());
    }

    /**
     * Gives the JSON object describing a single text input.
     */
    private static JSONObject getComponentDataForTextInput(String id, String name, String value, long maxLength, boolean isLong) {
        JSONObject row = new JSONObject();
        row.put("type", 1); // ... action row

        JSONObject component = new JSONObject();
        component.put("type", 4); // text input
        component.put("custom_id", id);
        component.put("label", name);
        component.put("style", isLong ? 2 : 1);
        component.put("min_length", 0);
        component.put("max_length", maxLength);
        component.put("required", false);
        component.put("value", value == null ? "" : value);

        JSONArray rowContent = new JSONArray();
        rowContent.put(component);
        row.put("components", rowContent);

        return row;
    }

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

    private static void respondLater(HttpServletResponse responseStream) throws IOException {
        JSONObject response = new JSONObject();
        response.put("type", 5); // deferred response in channel

        JSONObject responseData = new JSONObject();
        responseData.put("flags", 1 << 6); // ephemeral
        response.put("data", responseData);

        log.debug("Responding with: " + response.toString(2));
        responseStream.getWriter().write(response.toString());
    }

    private static void respondDeferred(String interactionToken, String message) throws IOException {
        String url = "https://discord.com/api/v10/webhooks/" + SecretConstants.OLYMPUS_NEWS_MANAGER_CLIENT_ID + "/" + interactionToken + "/messages/@original";

        JSONObject responseData = new JSONObject();
        responseData.put("content", message);
        responseData.put("allowed_mentions", new JSONObject("{\"parse\": []}"));

        // Apache HttpClient because PATCH does not exist according to HttpURLConnection
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch(new URI(url));
            httpPatch.setHeader("User-Agent", "DiscordBot (https://maddie480.ovh/discord-bots, 1.0)");
            httpPatch.setEntity(new StringEntity(responseData.toString(), ContentType.APPLICATION_JSON));
            CloseableHttpResponse httpResponse = httpClient.execute(httpPatch);

            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                log.error("Discord responded with " + httpResponse.getStatusLine().getStatusCode() + " to our edit request!");
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
