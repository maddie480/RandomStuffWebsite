package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
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
import java.util.stream.Stream;

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

                o.put("lookingForPeople", Stream.of("reqMappers", "reqCoders", "reqArtists",
                        "reqMusicians", "reqPlaytesters", "reqDecorators", "reqLobby").anyMatch(l -> !"no".equals(o.getString(l)))
                        || !o.getString("reqOther").isBlank());

                if (!"hidden".equals(o.getString("status"))) {
                    contents.add(o);
                }
            }
        }

        contents.sort((c1, c2) -> {
            if (!c1.getString("status").equals(c2.getString("status"))) {
                List<String> statusOrder = Arrays.asList("in-progress", "paused", "released", "cancelled");
                return statusOrder.indexOf(c1.getString("status")) - statusOrder.indexOf(c2.getString("status"));
            }

            if (c1.getBoolean("lookingForPeople") != c2.getBoolean("lookingForPeople")) {
                return (c1.getBoolean("lookingForPeople") ? 0 : 1) - (c2.getBoolean("lookingForPeople") ? 0 : 1);
            }

            return c1.getString("name").compareToIgnoreCase(c2.getString("name"));
        });

        request.setAttribute("collabs", contents);

        PageRenderer.render(request, response, "collab-list", "Celeste Collab & Contest List",
                "You can find info on ongoing Celeste collabs and contests here, updated by their managers.");
    }
}
