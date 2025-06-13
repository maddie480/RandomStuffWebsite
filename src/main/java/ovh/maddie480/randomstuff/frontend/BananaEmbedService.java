
package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GameBanana embeds, now with 100% less extended gaming!
 */
@WebServlet(name = "BananaEmbedService", urlPatterns = {"/gamebanana.com/*", "/celeste/banana-oembed/*"})
public class BananaEmbedService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(BananaEmbedService.class);

    private static final Path oEmbeds = Paths.get("/shared/temp/banana-oembeds");
    private static final Pattern typeAndIdExtractor = Pattern.compile("^/gamebanana\\.com/([a-z]+)s/([0-9]+)$");
    private static final Pattern oEmbedExtractor = Pattern.compile("^/celeste/banana-oembed/([a-z0-9-]+\\.json)$");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Matcher match = typeAndIdExtractor.matcher(request.getRequestURI());
        if (match.matches()) {
            String ua = request.getRequestProperty("User-Agent");
            if (ua != null && ua.contains("Discordbot")) {
                try {
                    String html = tryGenerateEmbed(match.group(1), match.group(2));
                    response.setContentType("text/html");
                    response.getWriter().write(html);
                    return;
                } catch (Exception e) {
                    log.warn("Could not generate embed for " + itemtype + " " + itemid + "! Falling back to redirect.", e);
                }
            }

            // the link we redirect to will match https://gamebanana\.com/[a-z]+s/[0-9]+
            response.sendRedirect("https:/" + request.getRequestURI());
            return;
        }

        match = oEmbedExtractor.matcher(request.getRequestURI());
        if (match.matches() && Files.isRegularFile(oEmbeds.resolve(match.group(1)))) {
            response.setContentType("application/json");
            try (InputStream is = Files.newInputStream(oEmbeds.resolve(match.group(1)))) {
                IOUtils.copy(is, response.getOutputStream());
            }
            return;
        }

        log.warn("Not found");
        response.setStatus(404);
        PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }

    private String tryGenerateEmbed(String itemtype, String itemid) throws IOException {
        if (Files.exists(oEmbeds.resolve(itemtype + "-" + itemid + ".html"))) {
            // cached embed!
            try (InputStream is = Files.newInputStream(oEmbeds.resolve(itemtype + "-" + itemid + ".html"));
                 ByteArrayOutputStream os = new ByteArrayOutputStream()) {

                IOUtils.copy(is, os);
                return new String(os.toByteArray(), StandardCharsets.UTF_8);
            }
        }

        String capitalizedItemtype = itemtype.substring(0, 1).toUpperCase() + itemtype.substring(1);

        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout("https://gamebanana.com/apiv11/" + capitalizedItemtype + "/" + itemid + "/ProfilePage");
        // we're on a tight schedule here
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(3000);

        JSONObject profilePage;
        try (InputStream is = ConnectionUtils.connectionToInputStream(connection.getInputStream())) {
            profilePage = new JSONObject(new JSONTokener(is));
        }

        JSONObject oEmbed = new JSONObject();
        oEmbed.put("type", "link");
        oEmbed.put("version", "1.0");
        oEmbed.put("title", profilePage.getString("_sName"));
        oEmbed.put("author_name", profilePage.getJSONObject("_aSubmitter").getString("_sName"));
        oEmbed.put("author_url", profilePage.getJSONObject("_aSubmitter").getString("_sProfileUrl"));
        oEmbed.put("provider_name", "GameBanana");
        oEmbed.put("provider_url", "https://gamebanana.com/");
        oEmbed.put("thumbnail_url", profilePage.getJSONObject("_aCategory").getString("_sIconUrl"));
        oEmbed.put("cache_age", 60);

        // the HTML is there to add a description, image and fancy color, and to link to the oEmbed above
        JSONObject firstImage = profilePage.getJSONObject("_aPreviewMedia").getJSONArray("_aImages").getJSONObject(0);
        String html = "<!DOCTYPE html>\n<html>\n<head>\n"
            + "<meta property=\"og:description\" content=\"" + escapeHtml4(profilePage.getString("_sDescription")) + "\">\n"
            + "<meta name=\"theme-color\" content=\"#FFE033\">\n"
            + "<meta property=\"og:image\" content=\"" + firstImage.getString("_sBaseUrl") + "/" + firstImage.getString("_sFile") + "\">\n"
            + "<link rel=\"alternate\" type=\"application/json+oembed\" href=\"https://maddie480.ovh/celeste/banana-oembed/" + itemtype + "-" + itemid + ".json\"/>"
            + "</head>\n<body>\nHi! What are you doing here?\n</body>\n</html>";

        // prepare for the call to the oembed url above...
        try (OutputStream os = Files.newOutputStream(oEmbeds.resolve(itemtype + "-" + itemid + ".json")) {
            oEmbed.write(os);
        }
        // ... and write cache of the html too
        try (OutputStream os = Files.newOutputStream(oEmbeds.resolve(itemtype + "-" + itemid + ".html")) {
            os.write(html.toByteArray(StandardCharsets.UTF_8));
        }
        return html;
    }
}