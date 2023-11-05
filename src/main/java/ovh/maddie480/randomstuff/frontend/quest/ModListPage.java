package ovh.maddie480.randomstuff.frontend.quest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.frontend.PageRenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ModListPage", urlPatterns = {"/quest/mods/", "/quest/mods/*"}, loadOnStartup = 9)
public class ModListPage extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ModListPage.class);

    private List<Mod> cache = null;

    @Override
    public void init() {
        try (InputStream is = ModListPage.class.getClassLoader().getResourceAsStream("resources/quest/quest-mod-manager/database.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            br.readLine();
            String s;
            List<Mod> mods = new ArrayList<>();
            while ((s = br.readLine()) != null) {
                mods.add(new Mod(s, false));
            }

            cache = mods;
            logger.debug("Fetched " + mods.size() + " mods");
        } catch (Exception e) {
            logger.warn("Warming up failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("error", cache == null);
        request.setAttribute("mods", cache);

        if (request.getRequestURI().startsWith("/quest/mods/")) {
            Mod matchingMod = cache.stream()
                    .filter(mod -> mod.getId().equals(normalize(request.getRequestURI().substring("/quest/mods/".length()))))
                    .findFirst().orElse(null);

            response.setStatus(302);
            if (matchingMod == null) {
                response.setHeader("Location", "/quest/mods");
            } else {
                response.setHeader("Location", matchingMod.getModUrl());
            }
        } else {
            PageRenderer.render(request, response, "quest-mods", "Liste des mods de Quest",
                    "La liste de tous les mods de Quest qui sont enregistrés dans QUEST Community Bot, et qui apparaissent dans Quest Mod Manager.");
        }
    }

    public static String normalize(String name) {
        name = StringUtils.stripAccents(name).toLowerCase();
        StringBuilder endName = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i)) || name.charAt(i) == '.' || Character.isDigit(name.charAt(i))) {
                endName.append(name.charAt(i));
            }
        }
        return endName.toString();
    }
}