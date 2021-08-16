package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://gamebanana.com/apiv5/Mod/ByCategory?_csvProperties=_sName,_sProfileUrl,_aPreviewMedia,_tsDateAdded&"
                + request.getQueryString()).openConnection();

        if (connection.getResponseCode() != 200) {
            // pass the answer through
            response.setStatus(connection.getResponseCode());
            response.setHeader("Content-Type", connection.getContentType());
            IOUtils.copy(connection.getErrorStream(), response.getOutputStream());
            return;
        }

        StringBuilder rss = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<rss version=\"2.0\">\n" +
                "\t<items>\n" +
                "\t\t<title>GameBanana API RSS Feed</title>\n");

        JSONArray modList = new JSONArray(IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8));

        for (Object item : modList) {
            JSONObject mod = (JSONObject) item;
            rss.append("\t\t<item>\n");
            rss.append("\t\t\t<title>").append(StringEscapeUtils.escapeXml10(mod.getString("_sName"))).append("</title>\n");
            rss.append("\t\t\t<link>").append(mod.getString("_sProfileUrl")).append("</link>\n");

            JSONObject image = mod.getJSONArray("_aPreviewMedia").getJSONObject(0);
            rss.append("\t\t\t<image>")
                    .append(image.getString("_sBaseUrl")).append("/").append(image.getString("_sFile"))
                    .append("</image>\n");

            rss.append("\t\t\t<pubDate>")
                    .append(Instant.ofEpochSecond(mod.getLong("_tsDateAdded")).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.RFC_1123_DATE_TIME))
                    .append("</pubDate>\n");
            rss.append("\t\t</item>\n");
        }
        rss.append("\t</items>\n</rss>");

        response.setHeader("Content-Type", "application/rss+xml");
        response.getWriter().write(rss.toString());
    }
}
