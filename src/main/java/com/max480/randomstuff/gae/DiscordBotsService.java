package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        Map<String, Integer> serverCounts = new Yaml().load(
                IOUtils.toString(CloudStorageUtils.getCloudStorageInputStream("bot_server_counts.yaml"), StandardCharsets.UTF_8));

        request.setAttribute("timezoneBotLiteServerCount", serverCounts.get("TimezoneBotLite"));
        request.setAttribute("timezoneBotFullServerCount", serverCounts.get("TimezoneBotFull"));
        request.setAttribute("modStructureVerifierServerCount", serverCounts.get("ModStructureVerifier"));
        request.setAttribute("gamesBotServerCount", serverCounts.get("GamesBot"));
        request.setAttribute("customSlashCommandsServerCount", serverCounts.get("CustomSlashCommands"));

        request.getRequestDispatcher("/WEB-INF/discord-bots.jsp").forward(request, response);
    }
}
