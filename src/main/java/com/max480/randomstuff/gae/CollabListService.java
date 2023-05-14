package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This service lists all ongoing Celeste collabs.
 */
@WebServlet(name = "CollabListService", urlPatterns = {"/celeste/collab-contest-list"})
@MultipartConfig
public class CollabListService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CollabListService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        List<JSONObject> contents = new ArrayList<>();

        for (String s : new File("/shared/celeste/collab-list").list()) {
            try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/collab-list/" + s))) {
                JSONObject o = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                if (!"hidden".equals(o.getString("status"))) {
                    contents.add(o);
                    o.put("notes", escapeHtmlAndHighlightLinks(o.getString("notes")));
                }
            }
        }

        contents.sort((c1, c2) -> {
            if (!c1.getString("status").equals(c2.getString("status"))) {
                List<String> statusOrder = Arrays.asList("in-progress", "paused", "released", "cancelled");
                return statusOrder.indexOf(c1.getString("status")) - statusOrder.indexOf(c2.getString("status"));
            }

            if (!c1.getString("lookingForPeople").equals(c2.getString("lookingForPeople"))) {
                return c2.getString("lookingForPeople").compareTo(c1.getString("lookingForPeople"));
            }

            return c1.getString("name").compareToIgnoreCase(c2.getString("name"));
        });

        request.setAttribute("collabs", contents);

        PageRenderer.render(request, response, "collab-list", "Celeste Collab & Contest List",
                "You can find info on ongoing Celeste collabs and contests here, updated by their managers.");
    }

    private static String escapeHtmlAndHighlightLinks(String source) {
        source = StringEscapeUtils.escapeHtml4(source);

        while (true) {
            int startIndex = Math.min(
                    source.contains("&lt;http://") ? source.indexOf("&lt;http://") : Integer.MAX_VALUE,
                    source.contains("&lt;https://") ? source.indexOf("&lt;https://") : Integer.MAX_VALUE
            );
            if (startIndex == Integer.MAX_VALUE) {
                // nothing looking like a link here
                break;
            }

            int endIndex = startIndex + source.substring(startIndex).indexOf("&gt;") + 4;
            if (endIndex == startIndex + 3) {
                // start of link has no end of link that matches
                break;
            }

            source = source.substring(0, startIndex)
                    + "<a href=\"" + source.substring(startIndex + 4, endIndex - 4) + "\" target=\"_blank\">"
                    + source.substring(startIndex + 4, endIndex - 4) + "</a>"
                    + source.substring(endIndex);
        }

        return source;
    }
}
