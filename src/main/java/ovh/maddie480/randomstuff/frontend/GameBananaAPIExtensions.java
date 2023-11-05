package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Here are... APIs that somehow extend GameBanana's APIs.
 * Contrary to the CelesteModSearchService ones, those are not restricted to Celeste.
 */
@WebServlet(name = "GameBananaAPIExtensions", urlPatterns = {"/gamebanana/rss-feed"})
public class GameBananaAPIExtensions extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(GameBananaAPIExtensions.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // if sorting by "last updated", <pubDate> should be the last updated date rather than the created date.
        String pubDateField = "_tsDateAdded";
        if (request.getQueryString().contains("_sOrderBy=_tsDateUpdated,")) {
            pubDateField = "_tsDateUpdated";
        }

        String url = "https://gamebanana.com/apiv8/Mod/ByCategory?_csvProperties=_sName,_sProfileUrl,_aPreviewMedia," + pubDateField + "&" + request.getQueryString();
        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(url);

        if (connection.getResponseCode() != 200) {
            log.warn("Non-200 status code returned!");

            // 4xx errors get turned into 400 Bad Request, 5xx errors and everything else get turned into 502 Bad Gateway
            response.setStatus(connection.getResponseCode() / 100 == 4 ? 400 : 502);
            response.setContentType("text/plain");

            response.getWriter().write("GameBanana responded with unexpected HTTP response code " + connection.getResponseCode() + ".\n" +
                    "Please check the GameBanana source URL directly for more details:\n" + url);

            return;
        }

        StringBuilder rss = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <rss version="2.0">
                \t<items>
                \t\t<title>GameBanana API RSS Feed</title>
                """);

        JSONArray modList = new JSONArray(IOUtils.toString(ConnectionUtils.connectionToInputStream(connection), StandardCharsets.UTF_8));

        for (Object item : modList) {
            JSONObject mod = (JSONObject) item;
            rss.append("\t\t<item>\n");
            rss.append("\t\t\t<title>").append(StringEscapeUtils.escapeXml10(mod.getString("_sName"))).append("</title>\n");
            rss.append("\t\t\t<link>").append(mod.getString("_sProfileUrl")).append("</link>\n");

            JSONObject image = mod.getJSONObject("_aPreviewMedia").getJSONArray("_aImages").getJSONObject(0);
            rss.append("\t\t\t<image>")
                    .append(image.getString("_sBaseUrl")).append("/").append(image.getString("_sFile"))
                    .append("</image>\n");

            rss.append("\t\t\t<pubDate>")
                    .append(Instant.ofEpochSecond(mod.getLong(pubDateField)).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.RFC_1123_DATE_TIME))
                    .append("</pubDate>\n");
            rss.append("\t\t</item>\n");
        }
        rss.append("\t</items>\n</rss>");

        response.setHeader("Content-Type", "application/rss+xml");
        response.getWriter().write(rss.toString());
    }
}
