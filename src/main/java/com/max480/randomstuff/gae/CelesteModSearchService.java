package com.max480.randomstuff.gae;

import org.yaml.snakeyaml.Yaml;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(name = "CelesteModSearchService", loadOnStartup = 1, urlPatterns = {"/celeste/gamebanana-search", "/celeste/gamebanana-search-reload"})
public class CelesteModSearchService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModSearchService");

    private static List<Mod> modDatabase = null;

    @Override
    public void init() {
        try {
            refreshModDatabase();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    private static class Mod {
        public String gameBananaType;
        public int gameBananaId;
        public String name;
        public List<String> authors;
        public String description;
        public String text;

        protected Mod() {
        }

        public Mod(HashMap<String, Object> map) {
            gameBananaType = (String) map.get("GameBananaType");
            gameBananaId = (Integer) map.get("GameBananaId");
            name = (String) map.get("Name");
            authors = ((List<Object>) map.get("Authors")).stream()
                    .map(Object::toString).collect(Collectors.toList());
            description = (String) map.get("Description");
            text = (String) map.get("Text");
        }
    }

    private static class ScoredMod extends Mod {
        public int score;

        public ScoredMod(Mod mod) {
            gameBananaType = mod.gameBananaType;
            gameBananaId = mod.gameBananaId;
            name = mod.name;
            authors = mod.authors;
            description = mod.description;
            text = mod.text;
            score = 0;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/celeste/gamebanana-search-reload")) {
            if (("key=" + Constants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshModDatabase();
            } else {
                // invalid secret
                response.setStatus(403);
            }
            return;
        }

        if (request.getParameter("q") == null) {
            // the user didn't give any search!
            response.setHeader("Content-Type", "text/plain");
            response.setStatus(400);
            response.getWriter().write("\"q\" query parameter expected");
        } else {
            response.setHeader("Content-Type", "text/yaml");

            // remove double spaces and switch it to lowercase.
            String query = request.getParameter("q").toLowerCase(Locale.ENGLISH);
            while (query.contains("  ")) {
                query = query.replace("  ", " ");
            }

            List<ScoredMod> scoredMods = modDatabase.stream().map(ScoredMod::new).collect(Collectors.toList());

            // now commit search!
            executeSearchOn(scoredMods, query, mod -> mod.name, 5);
            executeSearchOn(scoredMods, query, mod -> String.join(",", mod.authors), 3);
            executeSearchOn(scoredMods, query, mod -> mod.description, 2);
            executeSearchOn(scoredMods, query, mod -> mod.text, 1);

            // sort and aggregate the results.
            List<Map<String, Object>> responseBody = scoredMods.stream()
                    .filter(mod -> mod.score != 0)
                    .sorted(Comparator.comparingInt(mod -> ((ScoredMod) mod).score).reversed())
                    .limit(20)
                    .map(mod -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("itemtype", mod.gameBananaType);
                        result.put("itemid", mod.gameBananaId);
                        return result;
                    })
                    .collect(Collectors.toList());

            response.getWriter().write(new Yaml().dump(responseBody));
        }
    }

    // mapping takes an awful amount of time on App Engine (~2 seconds) so we can't make it when the user calls the API.
    private void refreshModDatabase() throws IOException {
        final List<Mod> mods;
        try (InputStream connectionToDatabase = new URL(Constants.MOD_SEARCH_DATABASE_URL).openStream()) {
            List<HashMap<String, Object>> loaded = new Yaml().load(connectionToDatabase);
            mods = loaded.stream().map(Mod::new).collect(Collectors.toList());
        }
        modDatabase = mods;
        logger.info("There are " + mods.size() + " mods in the search database.");
    }

    private void executeSearchOn(List<ScoredMod> mods, String query, Function<Mod, String> field, int score) {
        for (ScoredMod mod : mods) {
            String value = field.apply(mod).toLowerCase(Locale.ENGLISH);
            if (value.equals(query)) {
                // wow, exact match!
                mod.score += 4 * score;
            } else if (value.contains(query)) {
                // search is part of the mod field, so this is a good match.
                mod.score += 3 * score;
            } else {
                String[] words = query.split(" ");
                if (Arrays.stream(words).allMatch(value::contains)) {
                    // all words are found if searched separately... this is more of a dubious match.
                    mod.score += score;
                }
            }
        }
    }
}
