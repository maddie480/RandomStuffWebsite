package com.max480.randomstuff.gae.quest;

import com.max480.randomstuff.gae.PageRenderer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@WebServlet(name = "BackgroundListPage", urlPatterns = {"/quest/backgrounds", "/quest/backgrounds/*", "/quest/game_backgrounds/*"})
public class BackgroundListPage extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/quest/backgrounds/") || request.getRequestURI().startsWith("/quest/game_backgrounds/")) {
            Path source = Paths.get("/shared/temp/quest-backgrounds/" +
                    request.getRequestURI().substring(1, request.getRequestURI().lastIndexOf("/")) + "/" +
                    getProperName(request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/") + 1)));

            if (Files.isRegularFile(source)) {
                response.setContentType("image/png");

                try (InputStream is = Files.newInputStream(source);
                     OutputStream os = response.getOutputStream()) {

                    IOUtils.copy(is, os);
                }
            }

            return;
        }

        request.setAttribute("title", "Erreur");
        request.setAttribute("metaDescription", "Quelque chose s'est mal passé... Désolée - Maddie");

        request.setAttribute("page", "backgrounds");

        request.setAttribute("error", false);
        request.setAttribute("tokenExpired", false);

        if (request.getParameter("token") == null || !request.getParameter("token").matches("^[0-9]+$")) {
            request.setAttribute("tokenExpired", true);

            PageRenderer.render(request, response, "quest-backgrounds", "Liste des arrière-plans de profil (lien invalide)",
                    "C'est le moment de réutiliser la commande !backgrounds...");
            return;
        }

        Path dataFile = Paths.get("/shared/temp/quest-backgrounds/" + request.getParameter("token") + ".txt");

        if (Files.isRegularFile(dataFile)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(dataFile), UTF_8))) {
                Long monies = Long.parseLong(br.readLine());
                String pseudo = URLDecoder.decode(br.readLine(), UTF_8);

                List<Background> bgs = new ArrayList<>();

                String s;
                boolean owned = true;
                while ((s = br.readLine()) != null) {
                    if (s.isEmpty()) continue;

                    if (s.equals("===") && owned) {
                        owned = false;
                    } else if (s.equals("===")) {
                        break;
                    } else {
                        bgs.add(new Background(s, owned));
                    }
                }

                // lire les pseudos
                Map<String, String> pseudos = new HashMap<>();
                while ((s = br.readLine()) != null) {
                    if (s.equals("===")) break;
                    pseudos.put(s.split(";")[0], URLDecoder.decode(s.split(";")[1], UTF_8));
                }

                for (Background bg : bgs) {
                    bg.author = pseudos.get(bg.author);
                }

                // lire les arrière-plans de jeu
                List<GameBackground> gameBackgrounds = new ArrayList<>();
                while ((s = br.readLine()) != null) {
                    GameBackground gbg = new GameBackground();
                    if (s.startsWith("default;")) {
                        s = s.substring("default;".length());
                        gbg.defaultBg = true;
                    }
                    gbg.name = URLDecoder.decode(s.split(";")[0], UTF_8);
                    gbg.url = s.split(";", 2)[1];
                    gameBackgrounds.add(gbg);
                }

                request.setAttribute("pseudo", pseudo);
                request.setAttribute("credit", new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.FRENCH)).format(monies) +
                        (monies == 1 ? " pièce" : " pièces"));

                request.setAttribute("backgrounds", bgs);
                request.setAttribute("gameBackgrounds", gameBackgrounds);

                PageRenderer.render(request, response, "quest-backgrounds", "Liste des arrière-plans de profil",
                        "Cette page liste tous les arrière-plans disponibles, et les arrière-plans achetés par " + pseudo + ".");
            }
        } else {
            request.setAttribute("tokenExpired", true);

            PageRenderer.render(request, response, "quest-backgrounds", "Liste des arrière-plans de profil (lien invalide)",
                    "C'est le moment de réutiliser la commande !backgrounds...");
        }
    }

    private static String getProperName(String name) {
        if (!name.contains(";")) return name;

        String first = name.substring(0, name.indexOf(";"));
        first = URLEncoder.encode(URLDecoder.decode(first, UTF_8), UTF_8);
        return first + name.substring(name.indexOf(";"));
    }
}