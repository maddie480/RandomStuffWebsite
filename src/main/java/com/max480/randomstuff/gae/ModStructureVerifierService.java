package com.max480.randomstuff.gae;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Servlet allowing to submit files to the Mod Structure Verifier bot.
 */
@WebServlet(name = "ModStructureVerifierService", loadOnStartup = 3, urlPatterns = {"/celeste/mod-structure-verifier"})
@MultipartConfig
public class ModStructureVerifierService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("ModStructureVerifierService");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("badrequest", false);
        PageRenderer.render(request, response, "mod-structure-verifier", "Celeste Mod Structure Verifier",
                "This tool allows you to check the structure and dependencies of your Celeste mods.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("badrequest", false);

        if (!ServletFileUpload.isMultipartContent(request)) {
            // if not, we stop here
            request.setAttribute("badrequest", true);
            logger.warning("Bad request");
            response.setStatus(400);
        } else {
            ServletFileUpload upload = new ServletFileUpload();

            logger.fine("Parsing request...");

            String id = UUID.randomUUID().toString();

            // parse request
            boolean modZipFound = false;
            String assetsFolderName = null;
            String mapsFolderName = null;
            String chunkIndex = null;
            String chunkId = null;

            try {
                FileItemIterator iter = upload.getItemIterator(request);
                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    if (item.isFormField()) {
                        // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                        String fieldname = item.getFieldName();
                        String fieldvalue;
                        try (InputStream is = item.openStream()) {
                            fieldvalue = IOUtils.toString(is, UTF_8);
                        }

                        if ("assetFolderName".equals(fieldname)) {
                            assetsFolderName = fieldvalue;
                        } else if ("mapFolderName".equals(fieldname)) {
                            mapsFolderName = fieldvalue;
                        } else if ("chunkId".equals(fieldname)) {
                            chunkId = fieldvalue;
                        } else if ("chunkIndex".equals(fieldname)) {
                            chunkIndex = fieldvalue;
                        }
                    } else {
                        // Process form file field (input type="file").
                        String fieldname = item.getFieldName();
                        if ("zipFile".equals(fieldname)) {
                            modZipFound = true;

                            // stream file straight into a local file.
                            try (InputStream readerStream = item.openStream()) {
                                Files.copy(readerStream, Paths.get("/shared/temp/mod-structure-verify/" + id + ".zip"));
                            }
                        }
                    }
                }
            } catch (FileUploadException e) {
                logger.warning("Cannot parse request: " + e);
            }

            if (!modZipFound
                    || (mapsFolderName != null && !mapsFolderName.matches("^[A-Za-z0-9]*$"))
                    || (assetsFolderName != null && !assetsFolderName.matches("^[A-Za-z0-9]*$"))) {

                request.setAttribute("badrequest", true);
                logger.warning("Bad request: no file was sent in POST request, or one of the folder names has forbidden characters");
                response.setStatus(400);
            } else {
                if (chunkIndex != null) {
                    chunkId = handleChunkRequest("mod-structure-verify-" + id + ".zip", chunkIndex, chunkId);

                    if (chunkId == null) {
                        // chunk request was invalid
                        response.setStatus(400);
                        return;
                    } else if (!"31".equals(chunkIndex)) {
                        // didn't send the last chunk, respond with chunk ID
                        response.setContentType("text/plain; charset=UTF-8");
                        response.getWriter().write(chunkId);
                        return;
                    }
                }

                // create the task, and give the URL to the page that will allow to follow it
                // (at this point, we either have all 32 chunks uploaded, or the sending was not chunked)
                runModStructureVerifyTask(id, chunkId, assetsFolderName, mapsFolderName);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write("/celeste/task-tracker/mod-structure-verify/" + id);
                return;
            }
        }

        PageRenderer.render(request, response, "mod-structure-verifier", "Celeste Mod Structure Verifier",
                "This tool allows you to check the structure and dependencies of your Celeste mods.");
    }

    private void runModStructureVerifyTask(String id, String chunkId, String assetsFolderName, String mapsFolderName) throws IOException {
        // save timestamp marker (this will save that the task exists, and the timestamp at which it started)
        Files.write(Paths.get("/shared/temp/mod-structure-verify/" + id + "-timestamp.txt"), Long.toString(System.currentTimeMillis()).getBytes(UTF_8));

        if (chunkId != null) {
            // merge the chunks together, there should be exactly 32 of them
            logger.fine("Composing chunks with id " + chunkId + "...");

            try (OutputStream os = Files.newOutputStream(Paths.get("/shared/temp/mod-structure-verify/" + id + ".zip"))) {
                for (int i = 0; i < 32; i++) {
                    Path chunkPath = Paths.get("/shared/temp/mod-structure-verify/upload-chunk-" + chunkId + "-" + i + ".bin");
                    try (InputStream is = Files.newInputStream(chunkPath)) {
                        IOUtils.copy(is, os);
                    }
                    Files.delete(chunkPath);
                }
            }
        }

        logger.fine("Creating task...");

        // generate message payload
        JSONObject message = new JSONObject();
        message.put("taskType", "modStructureVerify");
        message.put("fileName", "mod-structure-verify-" + id + ".zip");

        if (assetsFolderName == null || assetsFolderName.isEmpty()) {
            // no path was given
            message.put("withPathsCheck", false);
        } else {
            // paths were given: enable paths check and pass parameters
            message.put("withPathsCheck", true);
            message.put("assetFolderName", assetsFolderName);

            if (mapsFolderName == null || mapsFolderName.isEmpty()) {
                // map folder name = asset folder name
                message.put("mapFolderName", assetsFolderName);
            } else {
                message.put("mapFolderName", mapsFolderName);
            }
        }

        // publish the message to the backend!
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("backend", 4480));
            try (OutputStream os = socket.getOutputStream()) {
                IOUtils.write(message.toString(), os, StandardCharsets.UTF_8);
            }
        }
    }

    private String handleChunkRequest(String tempFileName, String chunkIndex, String chunkId) throws IOException {
        if ("0".equals(chunkIndex)) {
            // generate a new chunk upload
            chunkId = UUID.randomUUID().toString();
        } else if (chunkId == null) {
            logger.warning("No chunk id was given and this is not the first chunk!");
            return null;
        } else if (!chunkId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            logger.warning("Chunk ID is not a UUID: " + chunkId);
            return null;
        } else {
            // check if the chunk index is in bounds and if the previous chunk exists.
            try {
                int chunkIndexInt = Integer.parseInt(chunkIndex);
                if (chunkIndexInt < 1 || chunkIndexInt > 31) {
                    logger.warning("Chunk index is outside of the allowed bounds: " + chunkIndexInt);
                    return null;
                } else {
                    if (!Files.exists(Paths.get("/shared/temp/mod-structure-verify/upload-chunk-" + chunkId + "-" + (chunkIndexInt - 1) + ".bin"))) {
                        logger.warning("Previous chunk upload-chunk-" + chunkId + "-" + (chunkIndexInt - 1) + ".bin does not exist!");
                        return null;
                    }
                }
            } catch (NumberFormatException e) {
                logger.warning("Chunk index is not an integer: " + chunkIndex);
                return null;
            }
        }

        // rename the file so that it's clear that it is a chunk, not a zip.
        logger.fine("Moving file to expected location...");
        Files.move(Paths.get(tempFileName), Paths.get("/shared/temp/mod-structure-verify/upload-chunk-" + chunkId + "-" + chunkIndex + ".bin"));

        return chunkId;
    }
}
