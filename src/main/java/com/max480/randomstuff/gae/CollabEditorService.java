package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
        if (request.getQueryString() == null || !request.getQueryString().startsWith("key=")
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
        if (request.getQueryString() == null || !request.getQueryString().startsWith("key=")
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

            request.setAttribute("bad_request", !valid);
            request.setAttribute("saved", valid);
            if (valid) {
                try (OutputStream os = Files.newOutputStream(Paths.get("/shared/celeste/collab-list/" + request.getQueryString().substring(4) + ".json"))) {
                    IOUtils.write(collabInfo.toString(), os, StandardCharsets.UTF_8);
                }
            }

            retrieveAndRespond(request, response);
        }
    }

    private static void retrieveAndRespond(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONObject collabInfo;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/collab-list/" + request.getQueryString().substring(4) + ".json"))) {
            collabInfo = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        for (String attribute : Arrays.asList("name", "status", "contact", "notes", "reqMappers", "reqCoders", "reqArtists",
                "reqMusicians", "reqPlaytesters", "reqDecorators", "reqLobby", "reqOther")) {

            request.setAttribute(attribute, collabInfo.getString(attribute));
        }

        PageRenderer.render(request, response, "collab-editor", "Celeste Collab/Contest Editor",
                "On this page, you can edit the information displayed about your collab/contest in the Collab & Contest List.");
    }
}
