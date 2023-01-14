package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * This servlet is just here to serve the Vue app.
 * The build script puts all the resources along with the Java app's resources, and moves Vue's index.html
 * into a vue-index.html within the project's classpath. So we just need to send that!
 */
@WebServlet(name = "VueServer", urlPatterns = {"/celeste/wipe-converter", "/celeste/banana-mirror-browser",
        "/celeste/map-tree-viewer", "/celeste/file-searcher"})
public class VueServer extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (InputStream is = VueServer.class.getClassLoader().getResourceAsStream("vue-index.html")) {
            String contents = IOUtils.toString(is, StandardCharsets.UTF_8);

            if (request.getRequestURI().equals("/celeste/banana-mirror-browser")) {
                contents = contents
                        .replace("${favicon}", StringEscapeUtils.escapeHtml4("/celeste/favicon.ico"))
                        .replace("${page_title}", StringEscapeUtils.escapeHtml4("Banana Mirror Browser"))
                        .replace("${page_description}", StringEscapeUtils.escapeHtml4(
                                "If GameBanana is down or being slow, you can download Celeste mods here instead."));
            } else if (request.getRequestURI().equals("/celeste/wipe-converter")) {
                contents = contents
                        .replace("${favicon}", StringEscapeUtils.escapeHtml4("/celeste/favicon.ico"))
                        .replace("${page_title}", StringEscapeUtils.escapeHtml4("Celeste Wipe Converter"))
                        .replace("${page_description}", StringEscapeUtils.escapeHtml4(
                                "Upload the frames of a custom Celeste screen wipe here, and you will be able to use it in-game with the \"max480's Helping Hand\" mod."));
            } else if (request.getRequestURI().equals("/celeste/file-searcher")) {
                contents = contents
                        .replace("${favicon}", StringEscapeUtils.escapeHtml4("/celeste/favicon.ico"))
                        .replace("${page_title}", StringEscapeUtils.escapeHtml4("Celeste File Searcher"))
                        .replace("${page_description}", StringEscapeUtils.escapeHtml4(
                                "Use this tool to find in which Celeste mod(s) a file is on GameBanana, based on its path in the zip."));
            } else {
                contents = contents
                        .replace("${favicon}", StringEscapeUtils.escapeHtml4("/celeste/favicon.ico"))
                        .replace("${page_title}", StringEscapeUtils.escapeHtml4("Celeste Map Tree Viewer"))
                        .replace("${page_description}", StringEscapeUtils.escapeHtml4("See the raw contents of your map .bin as a tree to find out what is inside!"));
            }

            response.getWriter().write(contents);
        }
    }
}
