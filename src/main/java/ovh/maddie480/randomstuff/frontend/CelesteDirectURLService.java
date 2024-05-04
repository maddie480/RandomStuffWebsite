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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CelesteDirectURLService", urlPatterns = {"/celeste/direct-link-service", "/celeste/dl", "/picrew",
        "/celeste/download-olympus", "/celeste/download-everest"})
public class CelesteDirectURLService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteDirectURLService.class);

    private static Map<String, String> dlUrls = new HashMap<>();
    private static Map<String, String> mirrorUrls = new HashMap<>();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/dl")) {
            String modId = request.getParameter("id");
            String twoclick = request.getParameter("twoclick");
            String mirror = request.getParameter("mirror");

            if (!dlUrls.containsKey(modId)) {
                notFound(request, response);
            } else {
                String downloadLink = ("1".equals(mirror) ? mirrorUrls : dlUrls).get(modId);

                if ("1".equals(twoclick)) {
                    response.sendRedirect("https://0x0a.de/twoclick?" + downloadLink.substring(8));
                } else {
                    response.sendRedirect(downloadLink);
                }
            }

        } else if (request.getRequestURI().equals("/celeste/direct-link-service")) {
            request.setAttribute("typedId", "");
            request.setAttribute("twoclick", false);
            request.setAttribute("mirror", false);
            request.setAttribute("bundle", false);

            PageRenderer.render(request, response, "direct-url-service", "Celeste Direct Link service",
                    "This page can give you direct download URLs to the latest version of a mod, based on the ID present in its everest.yaml file.");
        } else if (request.getRequestURI().equals("/picrew")) {
            // Hard-coded DIY URL shortener to the rescue
            response.sendRedirect("https://picrew.me/en/image_maker/1387003");

        } else if (request.getRequestURI().equals("/celeste/download-everest")) {
            JSONObject branch;

            try (BufferedReader br = Files.newBufferedReader(Paths.get("/shared/celeste/everest-versions-with-native.json"))) {
                JSONArray versions = new JSONArray(new JSONTokener(br));
                branch = getBranch(versions, request.getParameter("branch"));
            }

            if (branch != null) {
                response.sendRedirect(branch.getString("mainDownload"));
            } else {
                notFound(request, response);
            }

        } else if (request.getRequestURI().equals("/celeste/download-olympus")) {
            JSONObject branch;

            try (BufferedReader br = Files.newBufferedReader(Paths.get("/shared/celeste/olympus-versions.json"))) {
                JSONArray versions = new JSONArray(new JSONTokener(br));
                branch = getBranch(versions, request.getParameter("branch"));
            }

            if (branch != null && branch.has(request.getParameter("platform") + "Download")) {
                response.sendRedirect(branch.getString(request.getParameter("platform") + "Download"));
            } else {
                notFound(request, response);
            }
        } else {
            // How do you even land here?
            notFound(request, response);
        }
    }

    private JSONObject getBranch(JSONArray source, String branch) {
        for (int i = 0; i < source.length(); i++) {
            JSONObject candidate = source.getJSONObject(i);

            if (candidate.getString("branch").equals(branch)) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().equals("/celeste/direct-link-service")) {
            String modId = request.getParameter("modId");
            String twoclick = request.getParameter("twoclick");
            String mirror = request.getParameter("mirror");
            String bundle = request.getParameter("bundle");

            if (modId == null) {
                // handle this like a GET
                request.setAttribute("typedId", "");
                request.setAttribute("twoclick", false);
                request.setAttribute("mirror", false);
                request.setAttribute("bundle", false);
            } else {
                request.setAttribute("typedId", modId);
                request.setAttribute("twoclick", twoclick != null);
                request.setAttribute("mirror", mirror != null);
                request.setAttribute("bundle", bundle != null);

                // check if the link for this mod ID exists
                if (dlUrls.containsKey(modId)) {
                    if (bundle != null) {
                        request.setAttribute("link", "https://maddie480.ovh/celeste/bundle-download?id=" + URLEncoder.encode(modId, StandardCharsets.UTF_8));
                    } else {
                        request.setAttribute("link", "https://maddie480.ovh/celeste/dl?id=" + URLEncoder.encode(modId, StandardCharsets.UTF_8)
                                + (twoclick != null ? "&twoclick=1" : "")
                                + (mirror != null ? "&mirror=1" : ""));
                    }
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
            newDlUrls.put(element.getKey(), element.getValue().get("URL"));
            newMirrorUrls.put(element.getKey(), element.getValue().get("MirrorURL"));
        }

        log.debug("There are now {} URLs and {} mirror URLs.", newDlUrls.size(), newMirrorUrls.size());

        dlUrls = newDlUrls;
        mirrorUrls = newMirrorUrls;
    }

    private static void notFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.warn("Not found");
        response.setStatus(404);
        PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }
}
