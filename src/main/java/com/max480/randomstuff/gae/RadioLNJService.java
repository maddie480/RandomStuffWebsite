package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WebServlet(name = "RadioLNJService", urlPatterns = {"/radio-lnj", "/radio-lnj/playlist.json"}, loadOnStartup = 8)
public class RadioLNJService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(RadioLNJService.class);

    private List<JSONObject> playlist;
    private long nextItemStartsAt;

    @Override
    public void init() {
        try (InputStream is = RadioLNJService.class.getClassLoader().getResourceAsStream("radio_lnj_meta.json")) {
            playlist = new ArrayList<>();

            JSONArray array = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
            for (Object o : array) {
                JSONObject item = (JSONObject) o;
                item.put("duration", item.getInt("duration") + 5000);
                playlist.add(item);
            }

            Collections.shuffle(playlist);
            nextItemStartsAt = System.currentTimeMillis() + playlist.get(0).getInt("duration");
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/radio-lnj/playlist.json")) {
            synchronized (playlist) {
                while (nextItemStartsAt < System.currentTimeMillis()) {
                    JSONObject pastItem = playlist.remove(0);
                    nextItemStartsAt += playlist.get(0).getInt("duration");
                    playlist.add(pastItem);
                }

                int timeLeft = (int) (nextItemStartsAt - System.currentTimeMillis());

                JSONObject body = new JSONObject();
                body.put("seek", playlist.get(0).getInt("duration") - timeLeft);
                body.put("playlist", playlist);

                response.setContentType("application/json");
                response.getWriter().write(body.toString());
            }
        } else {
            PageRenderer.render(request, response, "radio-lnj", "Radio LNJ",
                    "La radio de référence pour vous enjailler sur des musiques de navets !");
        }
    }
}
