package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Servlet allowing to generate bitmap fonts for usage in Celeste (~~and any other game using the XML output of BMFont actually~~).
 */
@WebServlet(name = "CelesteFontGeneratorService", urlPatterns = {"/celeste/font-generator"})
@MultipartConfig
public class CelesteFontGeneratorService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteFontGeneratorService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("badrequest", false);

        PageRenderer.render(request, response, "font-generator", "Celeste Font Generator",
                "This tool allows you to generate bitmap fonts in a format appropriate for Celeste mods.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("badrequest", false);
        boolean sentZip = false;

        if (!request.getContentType().startsWith("multipart/form-data")) {
            // if not, we stop here
            request.setAttribute("badrequest", true);
            log.warn("Bad request");
            response.setStatus(400);
        } else {
            // parse request
            String font = null;
            String fontFileName = null;
            String dialogFile = null;

            String customFontFileName = null;
            InputStream customFontFile = null;

            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() == null) {
                    // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                    String fieldname = part.getName();
                    String fieldvalue = IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8);

                    if ("font".equals(fieldname)) {
                        font = fieldvalue;
                    } else if ("fontFileName".equals(fieldname)) {
                        fontFileName = fieldvalue;
                    }
                } else {
                    // Process form file field (input type="file").
                    String fieldname = part.getName();
                    InputStream filecontent = part.getInputStream();

                    if ("dialogFile".equals(fieldname)) {
                        dialogFile = IOUtils.toString(filecontent, StandardCharsets.UTF_8);
                    } else if ("fontFile".equals(fieldname)) {
                        customFontFileName = part.getSubmittedFileName();
                        if (customFontFileName.replace("\\", "/").contains("/")) {
                            customFontFileName = customFontFileName.substring(customFontFileName.replace("\\", "/").indexOf("/") + 1);
                        }
                        customFontFile = filecontent;
                    }
                }
            }

            if (font == null || dialogFile == null) {
                request.setAttribute("badrequest", true);
                log.warn("Bad request");
                response.setStatus(400);
            } else {
                if (font.equals("custom")) {
                    if (fontFileName == null || hasForbiddenCharacter(fontFileName)
                            || customFontFileName == null || hasForbiddenCharacter(customFontFileName)) {

                        request.setAttribute("badrequest", true);
                        log.warn("Bad request for custom font generation");
                        response.setStatus(400);
                    } else {
                        // create the task, and redirect to the page that will allow to follow it
                        String id = runBMFontCustomTask(dialogFile, fontFileName, customFontFile, customFontFileName);
                        response.setStatus(302);
                        response.setHeader("Location", "/celeste/task-tracker/font-generate/" + id);
                        return;
                    }
                } else {
                    // create the task, and redirect to the page that will allow to follow it
                    String id = runBMFontTask(font, dialogFile);
                    response.setStatus(302);
                    response.setHeader("Location", "/celeste/task-tracker/font-generate/" + id);
                    return;
                }
            }
        }

        if (!sentZip) {
            // render the HTML page.
            PageRenderer.render(request, response, "font-generator", "Celeste Font Generator",
                    "This tool allows you to generate bitmap fonts in a format appropriate for Celeste mods.");
        }
    }

    private String runBMFontTask(String language, String dialogFile) throws IOException {
        String id = intializeTask(dialogFile);

        // generate message payload
        JSONObject message = new JSONObject();
        message.put("taskType", "fontGenerate");
        message.put("language", language);
        message.put("fileName", "font-generate/" + id + ".txt");

        // publish the message to the backend!
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.1.1", 44480));
            try (OutputStream os = socket.getOutputStream()) {
                IOUtils.write(message.toString(), os, StandardCharsets.UTF_8);
            }
        }

        log.info("Emitted message to handle font generation task {}!", id);
        return id;
    }

    private String runBMFontCustomTask(String dialogFile, String fontFileName, InputStream customFontFile, String customFontFileName) throws IOException {
        String id = intializeTask(dialogFile);

        // store custom font file
        Path customFontFilePath = Paths.get("/shared/temp/font-generate/" + id + "-font"
                + (customFontFileName.contains(".") ? customFontFileName.substring(customFontFileName.lastIndexOf(".")) : ""));

        try (InputStream is = customFontFile;
             OutputStream os = Files.newOutputStream(customFontFilePath)) {

            IOUtils.copy(is, os);
        }

        // generate message payload
        JSONObject message = new JSONObject();
        message.put("taskType", "customFontGenerate");
        message.put("textFileName", "font-generate/" + id + ".txt");
        message.put("fontFileName", "font-generate/" + customFontFilePath.getFileName());
        message.put("resultFontFileName", fontFileName);

        // publish the message to the backend!
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.1.1", 44480));
            try (OutputStream os = socket.getOutputStream()) {
                IOUtils.write(message.toString(), os, StandardCharsets.UTF_8);
            }
        }

        log.info("Emitted message to handle custom font generation task {}!", id);
        return id;
    }

    private static String intializeTask(String dialogFile) throws IOException {
        String id = UUID.randomUUID().toString();

        // store timestamp marker (this will save that the task exists, and the timestamp at which it started)
        Path file = Paths.get("/shared/temp/font-generate/" + id + "-timestamp.txt");
        Files.writeString(file, Long.toString(System.currentTimeMillis()));

        // store dialog file
        file = Paths.get("/shared/temp/font-generate/" + id + ".txt");
        Files.writeString(file, dialogFile);

        return id;
    }

    /**
     * Checks if a file name contains characters that are forbidden in file names.
     */
    private static boolean hasForbiddenCharacter(String name) {
        return name.contains("/") || name.contains("\\") || name.contains("*") || name.contains("?")
                || name.contains(":") || name.contains("\"") || name.contains("<") || name.contains(">")
                || name.contains("|") || name.contains("\r") || name.contains("\n");
    }
}
