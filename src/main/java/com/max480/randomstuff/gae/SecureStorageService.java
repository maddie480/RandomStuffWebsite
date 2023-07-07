package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A simple storage secured with HTTP Basic authentication.
 */
@WebServlet(name = "SecureStorageService", urlPatterns = {"/secure-storage/*"})
public class SecureStorageService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(SecureStorageService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (!request.getRequestURI().matches("^/secure-storage/[A-Za-z0-9._-]+\\.[a-z]+$")) {
            // URL syntax invalid
            log.warn("Not found");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");

        } else if (request.getHeader("Authorization") == null) {
            // need basic auth
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "basic");
        } else {
            String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(SecretConstants.SECURE_STORAGE_READ_BASIC_AUTH.getBytes(StandardCharsets.UTF_8));

            if (basicAuthHeader.equals(request.getHeader("Authorization"))) {
                Path storage = Paths.get("/shared/secure-storage");
                String fileName = request.getRequestURI().substring(16);
                if (Files.isRegularFile(storage.resolve(fileName))) {
                    String extension = request.getRequestURI().substring(request.getRequestURI().lastIndexOf(".") + 1);
                    response.setContentType(StaticAssetsAndRouteNotFoundServlet.CONTENT_TYPES.get(extension));

                    try (InputStream is = Files.newInputStream(storage.resolve(fileName))) {
                        IOUtils.copy(is, response.getOutputStream());
                    }
                } else {
                    // targeted file does not exist
                    response.setStatus(404);
                    response.setHeader("Content-Type", "text/plain; charset=UTF-8");
                    response.getWriter().write("Not Found");
                }
            } else {
                // incorrect auth header
                log.warn("Forbidden");
                response.setStatus(403);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getRequestURI().equals("/secure-storage/contents.zip")) {
            String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(SecretConstants.SECURE_STORAGE_WRITE_BASIC_AUTH.getBytes(StandardCharsets.UTF_8));

            if (basicAuthHeader.equals(request.getHeader("Authorization"))) {
                Path target = Paths.get("/shared/secure-storage");
                log.debug("Cleaning target directory");
                FileUtils.cleanDirectory(target.toFile());

                try (ZipInputStream zip = new ZipInputStream(request.getInputStream())) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        log.debug("Storing file {}", entry.getName());
                        try (OutputStream os = Files.newOutputStream(target.resolve(entry.getName()))) {
                            IOUtils.copy(zip, os);
                        }
                    }
                }

                log.debug("Upload done!");
                response.getWriter().write("OK");
            } else {
                // invalid auth header
                log.warn("Forbidden");
                response.setStatus(403);
            }
        } else {
            // only /secure-storage/contents.zip exists
            log.warn("Not found");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        }
    }
}
