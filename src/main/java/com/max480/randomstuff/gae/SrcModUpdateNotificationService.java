package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.max480.randomstuff.backend.celeste.crontabs.UpdateCheckerTracker.ModInfo;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Allows the speedrun.com moderator team to manage which mods they want to be notified about.
 */
@WebServlet(name = "SrcModUpdateNotificationService", urlPatterns = {"/celeste/src-mod-update-notifications"})
@MultipartConfig
public class SrcModUpdateNotificationService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("SrcModUpdateNotificationService");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("access_forbidden", false);
        request.setAttribute("bad_request", false);
        request.setAttribute("bad_mod", false);
        request.setAttribute("not_registered", false);
        request.setAttribute("already_registered", false);
        request.setAttribute("register_success", false);
        request.setAttribute("unregister_success", false);

        if (("key=" + SecretConstants.SRC_MOD_LIST_KEY).equals(request.getQueryString())) {
            populateModList(request);
            PageRenderer.render(request, response, "src-mod-update-notifications", "speedrun.com mod update notifications",
                    "This page allows speedrun.com moderators to pick the mods they want to be notified about when they are updated.");
        } else {
            logger.warning("Invalid key");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("access_forbidden", false);
        request.setAttribute("bad_request", false);
        request.setAttribute("bad_mod", false);
        request.setAttribute("not_registered", false);
        request.setAttribute("already_registered", false);
        request.setAttribute("register_success", false);
        request.setAttribute("unregister_success", false);

        if (("key=" + SecretConstants.SRC_MOD_LIST_KEY).equals(request.getQueryString())) {
            String modId = request.getParameter("modId");
            String action = request.getParameter("action");

            if (modId == null || action == null) {
                logger.warning("Missing parameter!");
                request.setAttribute("bad_request", true);

            } else {
                List<String> modList;
                Path modUpdateNotificationIdsFile = Paths.get("/shared/celeste/src-mod-update-notification-ids.json");

                try (InputStream is = Files.newInputStream(modUpdateNotificationIdsFile)) {
                    modList = new JSONArray(IOUtils.toString(is, UTF_8)).toList()
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.toCollection(ArrayList::new));
                }

                boolean save = false;

                if (action.equals("Add to List")) {
                    if (modList.contains(modId)) {
                        logger.warning("Mod already registered: " + modId);
                        request.setAttribute("already_registered", true);

                    } else if (doesModExist(modId)) {
                        logger.info("New mod registered: " + modId);
                        modList.add(modId);
                        save = true;
                        request.setAttribute("register_success", true);

                    } else {
                        logger.warning("Mod does not exist: " + modId);
                        request.setAttribute("bad_mod", true);
                    }
                } else {
                    if (modList.contains(modId)) {
                        logger.info("Mod unregistered: " + modId);
                        modList.remove(modId);
                        save = true;
                        request.setAttribute("unregister_success", true);

                    } else {
                        logger.warning("Mod is not registered: " + modId);
                        request.setAttribute("not_registered", true);
                    }
                }

                if (save) {
                    Files.writeString(modUpdateNotificationIdsFile, new JSONArray(modList).toString());
                }
            }

            populateModList(request);
            PageRenderer.render(request, response, "src-mod-update-notifications", "speedrun.com mod update notifications",
                    "This page allows speedrun.com moderators to pick the mods they want to be notified about when they are updated.");
        } else {
            logger.warning("Invalid key");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        }
    }

    private void populateModList(HttpServletRequest request) throws IOException {
        List<String> modList;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/src-mod-update-notification-ids.json"))) {
            modList = new JSONArray(IOUtils.toString(is, UTF_8)).toList()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Map<String, Map<String, Object>> modUpdateDatabase;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/everest-update.yaml"))) {
            modUpdateDatabase = YamlUtil.load(is);
        }

        request.setAttribute("modList", modList.stream()
                .map(modId -> {
                    Map<String, String> result = new HashMap<>();

                    result.put("id", modId);
                    if (modUpdateDatabase.containsKey(modId)) {
                        Map<String, Object> itemFromDatabase = modUpdateDatabase.get(modId);
                        ModInfo modInfo = CelesteModSearchService.getModInfoByTypeAndId(
                                (String) itemFromDatabase.get("GameBananaType"), (int) itemFromDatabase.get("GameBananaId"));

                        result.put("url", (String) modInfo.fullInfo.get("PageURL"));
                        result.put("name", (String) modInfo.fullInfo.get("Name"));
                        result.put("version", (String) itemFromDatabase.get("Version"));
                    }

                    return result;
                })
                .collect(Collectors.toList()));
    }

    private boolean doesModExist(String modName) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/everest-update.yaml"))) {
            Map<String, Object> database = YamlUtil.load(is);
            return database.containsKey(modName);
        }
    }
}
