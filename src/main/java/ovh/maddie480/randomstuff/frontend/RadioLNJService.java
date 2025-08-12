package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;

@WebServlet(name = "RadioLNJService", urlPatterns = {"/radio-lnj", "/radio-lnj/playlist.json", "/radio-lnj/playlist.m3u", "/radio-lnj/playlist"}, loadOnStartup = 8)
public class RadioLNJService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(RadioLNJService.class);

    private List<JSONObject> playlist;
    private List<JSONObject> sortedPlaylist;
    private long nextItemStartsAt;

    private int elementCount;
    private String totalDuration;

    @Override
    public void init() {
        try (InputStream is = RadioLNJService.class.getClassLoader().getResourceAsStream("radio_lnj_meta.json")) {
            playlist = new ArrayList<>();

            JSONArray array = new JSONArray(new JSONTokener(is));
            for (Object o : array) {
                JSONObject item = (JSONObject) o;
                item.put("duration", item.getInt("duration") + 1000);
                playlist.add(item);
            }

            Collections.shuffle(playlist);
            nextItemStartsAt = System.currentTimeMillis() + playlist.get(0).getInt("duration");

            elementCount = playlist.size();

            int totalDurationMinutes = (int) (playlist.stream()
                    .mapToLong(element -> element.getInt("duration"))
                    .sum() / 60000L);

            totalDuration = totalDurationMinutes / 60 + "h"
                    + new DecimalFormat("00").format(totalDurationMinutes % 60);

            sortedPlaylist = new ArrayList<>(playlist);
            sortedPlaylist.sort(Comparator.comparing(item -> item.getString("trackName").toLowerCase(Locale.ROOT)));

            log.info("Loaded Radio LNJ playlist, " + elementCount + " elements, total duration " + totalDuration
                    + ", head of playlist is " + playlist.get(0) + " until " + Instant.ofEpochMilli(nextItemStartsAt));
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/radio-lnj/playlist.json")) {
            synchronized (playlist) {
                updatePlaylistPosition();

                int timeLeft = (int) (nextItemStartsAt - System.currentTimeMillis());

                JSONObject body = new JSONObject();
                body.put("seek", playlist.get(0).getInt("duration") - timeLeft);
                body.put("playlist", playlist);

                response.setContentType("application/json");
                body.write(response.getWriter());
            }
        } else if (request.getRequestURI().equals("/radio-lnj/playlist.m3u")) {
            synchronized (playlist) {
                updatePlaylistPosition();

                response.setContentType("text/plain");
                Writer output = response.getWriter();
                for (JSONObject element : playlist) {
                    output.write("https://maddie480.ovh" + element.getString("path") + "\r\n");
                }
            }
        } else {
            request.setAttribute("elementCount", elementCount);
            request.setAttribute("totalDuration", totalDuration);
            request.setAttribute("elements", sortedPlaylist);

            if (request.getRequestURI().equals("/radio-lnj/playlist")) {
                PageRenderer.render(request, response, "radio-lnj-playlist", "Radio LNJ – Playlist",
                        "Consultez la liste des chansons qui passent sur la Radio LNJ, et écoutez celles que vous voulez !");
            } else {
                PageRenderer.render(request, response, "radio-lnj", "Radio LNJ",
                        "La radio de référence pour vous enjailler sur des musiques de navets !");
            }
        }
    }

    private void updatePlaylistPosition() {
        while (nextItemStartsAt < System.currentTimeMillis()) {
            JSONObject pastItem = playlist.remove(0);
            nextItemStartsAt += playlist.get(0).getInt("duration");
            playlist.add(pastItem);
            log.info("Updated playlist: head of playlist is now " + playlist.get(0) + " until " + Instant.ofEpochMilli(nextItemStartsAt));
        }
    }
}
