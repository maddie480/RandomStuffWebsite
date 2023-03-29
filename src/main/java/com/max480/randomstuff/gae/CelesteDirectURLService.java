package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CelesteDirectURLService", urlPatterns = {"/celeste/direct-link-service",
        "/celeste/dl/*", "/celeste/mirrordl/*", "/picrew"})
public class CelesteDirectURLService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteDirectURLService.class);

    private static Map<String, String> dlUrls = new HashMap<>();
    private static Map<String, String> mirrorUrls = new HashMap<>();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().startsWith("/celeste/dl/")) {
            redirect(request, response, dlUrls, request.getRequestURI().substring(12));
        } else if (request.getRequestURI().startsWith("/celeste/mirrordl/")) {
            redirect(request, response, mirrorUrls, request.getRequestURI().substring(18));
        } else if (request.getRequestURI().equals("/celeste/direct-link-service")) {
            request.setAttribute("typedId", "");
            PageRenderer.render(request, response, "direct-url-service", "Celeste Direct Link service",
                    "This page can give you direct download URLs to the latest version of a mod, based on the ID present in its everest.yaml file.");
        } else if (request.getRequestURI().equals("/picrew")) {
            // Hard-coded DIY URL shortener to the rescue
            response.sendRedirect("https://picrew.me/share?cd=Eqzx6FYwjm");
        } else {
            // How do you even land here?
            notFound(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().equals("/celeste/direct-link-service")) {
            String modId = request.getParameter("modId");
            if (modId == null) {
                // handle this like a GET
                request.setAttribute("typedId", "");
            } else {
                request.setAttribute("typedId", modId);

                // check if the link for this mod ID exists
                String dasherized = CelesteModCatalogService.dasherize(modId);
                if (dlUrls.containsKey(dasherized)) {
                    request.setAttribute("link", dasherized);
                } else {
                    request.setAttribute("notfound", true);
                }
            }

            PageRenderer.render(request, response, "direct-url-service", "Celeste Direct Link service",
                    "This page can give you direct download URLs to the latest version of a mod, based on the ID present in its everest.yaml file.");
        } else {
            notFound(request, response);
        }
    }

    public static void updateUrls() throws IOException {
        Map<String, String> newDlUrls = new HashMap<>();
        Map<String, String> newMirrorUrls = new HashMap<>();

        Map<String, Map<String, String>> everestUpdate;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/updater/everest-update.yaml"))) {
            everestUpdate = YamlUtil.load(is);
        }

        for (Map.Entry<String, Map<String, String>> element : everestUpdate.entrySet()) {
            newDlUrls.put(CelesteModCatalogService.dasherize(element.getKey()), element.getValue().get("URL").substring(8));
            newMirrorUrls.put(CelesteModCatalogService.dasherize(element.getKey()), element.getValue().get("MirrorURL").substring(8));
        }

        log.debug("There are now {} URLs and {} mirror URLs.", newDlUrls.size(), newMirrorUrls.size());

        dlUrls = newDlUrls;
        mirrorUrls = newMirrorUrls;
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, Map<String, String> links, String redirectTo)
            throws IOException, ServletException {

        if (links.containsKey(redirectTo)) {
            response.sendRedirect("https://0x0a.de/twoclick?" + links.get(redirectTo));
        } else {
            notFound(request, response);
        }
    }

    private static void notFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.warn("Not found");
        response.setStatus(404);
        PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }
}
