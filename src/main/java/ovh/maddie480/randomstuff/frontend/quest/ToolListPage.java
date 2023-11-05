package ovh.maddie480.randomstuff.frontend.quest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@WebServlet(name = "ToolListPage", urlPatterns = {"/quest/tools", "/quest/tools/*"}, loadOnStartup = 10)
public class ToolListPage extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ToolListPage.class);

    private List<Tool> cache = null;

    @Override
    public void init() {
        try (InputStream is = ModListPage.class.getClassLoader().getResourceAsStream("quest-tool-database.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            br.readLine();
            String s;
            List<Tool> tools = new ArrayList<>();
            while ((s = br.readLine()) != null) {
                tools.add(new Tool(s));
            }

            cache = tools;
            logger.debug("Fetched " + tools.size() + " tools");
        } catch (Exception e) {
            logger.warn("Warming up failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("error", cache == null);
        request.setAttribute("tools", cache);

        if (request.getRequestURI().startsWith("/quest/tools/")) {
            Tool matchingTool = cache.stream()
                    .filter(tool -> ModListPage.normalize(tool.name).equals(ModListPage.normalize(request.getRequestURI().substring("/quest/tools/".length()))))
                    .findFirst().orElse(null);

            response.setStatus(302);
            if (matchingTool == null) {
                response.setHeader("Location", "/quest/tools");
            } else {
                response.setHeader("Location", matchingTool.downloadUrl);
            }
        } else {
            PageRenderer.render(request, response, "quest-tools", "Liste des outils et logiciels de la communauté",
                    "La liste de tous les outils de modding et autres logiciels autour de Quest qui sont " +
                            "enregistrés par QUEST Community Bot (commandes !list_tools et !info_tool).");
        }
    }

    private String replace(String string) {
        return string == null ? null : string
                .replace("<@354341658352943115>", "Maddie")
                .replace("<#349612269828702208>", "#mods_corruptions");
    }
}