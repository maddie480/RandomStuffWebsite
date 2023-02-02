package com.max480.randomstuff.gae;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * A page with information on Discord bots.
 * The dynamic part of the page is just the server count of each bot.
 */
@WebServlet(name = "DiscordBotsService", urlPatterns = {"/discord-bots"})
@MultipartConfig
public class DiscordBotsService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, Integer> serverCounts;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/discord-bots/bot-server-counts.yaml"))) {
            serverCounts = YamlUtil.load(is);
        }

        request.setAttribute("timezoneBotLiteServerCount", serverCounts.get("TimezoneBotLite"));
        request.setAttribute("timezoneBotFullServerCount", serverCounts.get("TimezoneBotFull"));
        request.setAttribute("modStructureVerifierServerCount", serverCounts.get("ModStructureVerifier"));
        request.setAttribute("gamesBotServerCount", serverCounts.get("GamesBot"));
        request.setAttribute("customSlashCommandsServerCount", serverCounts.get("CustomSlashCommands"));

        PageRenderer.render(request, response, "discord-bots", "Discord Bots",
                "Read more about the Mod Structure Verifier, the Timezone Bot, the Games Bot and the Custom Slash Commands application here.");
    }
}
