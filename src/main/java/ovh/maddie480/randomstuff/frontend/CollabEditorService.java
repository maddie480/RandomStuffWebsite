package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * This service allows collab managers to edit their collab's information.
 */
@WebServlet(name = "CollabEditorService", urlPatterns = {"/celeste/collab-contest-editor"})
@MultipartConfig
public class CollabEditorService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CollabEditorService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getQueryString() == null || !request.getQueryString().matches("^key=[0-9a-f]+$")
                || !new File("/shared/celeste/collab-list/" + request.getQueryString().substring(4) + ".json").exists()) {

            log.warn("Invalid key");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        } else {
            request.setAttribute("bad_request", false);
            request.setAttribute("saved", false);
            retrieveAndRespond(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getQueryString() == null || !request.getQueryString().matches("^key=[0-9a-f]+$")
                || !new File("/shared/celeste/collab-list/" + request.getQueryString().substring(4) + ".json").exists()) {

            log.warn("Invalid key");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        } else {
            JSONObject collabInfo = new JSONObject();

            boolean valid = true;
            for (String attribute : Arrays.asList("name", "status", "contact", "notes", "reqMappers", "reqCoders", "reqArtists",
                    "reqMusicians", "reqPlaytesters", "reqDecorators", "reqLobby", "reqOther")) {

                if (request.getParameter(attribute) == null
                        || (!Arrays.asList("reqOther", "notes").contains(attribute) && request.getParameter(attribute).isBlank())
                        || (attribute.equals("status") && !Arrays.asList("in-progress", "released", "paused", "cancelled", "hidden").contains(request.getParameter(attribute)))
                        || (attribute.startsWith("req") && !attribute.equals("reqOther") && !Arrays.asList("yes", "no", "maybe").contains(request.getParameter(attribute)))) {
                    valid = false;
                    break;
                }

                collabInfo.put(attribute, request.getParameter(attribute));
            }

            collabInfo.put("lookingForPeople", request.getParameter("lookingForPeople") != null ? "yes" : "no");

            request.setAttribute("bad_request", !valid);
            request.setAttribute("saved", valid);
            if (valid) {
                try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("/shared/celeste/collab-list/" + request.getQueryString().substring(4) + ".json"))) {
                    collabInfo.write(bw);
                }
            }

            retrieveAndRespond(request, response);
        }
    }

    private static void retrieveAndRespond(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONObject collabInfo;
        try (BufferedReader br = Files.newBufferedReader(Paths.get("/shared/celeste/collab-list/" + request.getQueryString().substring(4) + ".json"))) {
            collabInfo = new JSONObject(new JSONTokener(br));
        }

        for (String attribute : Arrays.asList("name", "status", "contact", "notes", "reqMappers", "reqCoders", "reqArtists",
                "reqMusicians", "reqPlaytesters", "reqDecorators", "reqLobby", "reqOther", "lookingForPeople")) {

            request.setAttribute(attribute, collabInfo.getString(attribute));
        }

        PageRenderer.render(request, response, "collab-editor", "Celeste Collab/Contest Editor",
                "On this page, you can edit the information displayed about your collab/contest in the Collab & Contest List.");
    }
}
