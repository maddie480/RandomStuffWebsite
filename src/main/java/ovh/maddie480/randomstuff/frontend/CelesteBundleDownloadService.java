package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.model.DependencyRecord;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.ModDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@WebServlet(name = "CelesteBundleDownloadService", urlPatterns = {"/celeste/bundle-download"})
public class CelesteBundleDownloadService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteBundleDownloadService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String modId = request.getParameter("id");

        if (modId != null) {
            Map<String, String> downloads = resolveDownloadLinksFor(modId);

            if (downloads.size() == 1) {
                // mod with no dependency: just redirect to the download
                response.sendRedirect("https://maddie480.ovh/celeste/dl?id=" + modId);
                return;

            } else if (downloads.size() > 1) {
                log.info("Going to download following files for mod ID {}: {}", modId, downloads);

                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + modId + "-Bundle.zip\"");

                try (ZipOutputStream os = new ZipOutputStream(response.getOutputStream())) {
                    // we don't need to compress zips
                    os.setLevel(0);

                    for (Map.Entry<String, String> download : downloads.entrySet()) {
                        log.debug("Adding {}.zip from {}", download.getKey(), download.getValue());

                        os.putNextEntry(new ZipEntry(download.getKey() + ".zip"));
                        try (InputStream is = ConnectionUtils.openStreamWithTimeout(download.getValue())) {
                            IOUtils.copy(is, os);
                        }
                        os.closeEntry();
                    }
                }

                log.debug("Transfer of {}.zip is done!", modId);
                return;
            }
        }

        // either we didn't pass "id", or the mod we passed there was not found.
        log.warn("Not found");
        response.setStatus(404);
        PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }

    private static Map<String, String> resolveDownloadLinksFor(String modId) throws IOException {
        Map<String, String> result = new HashMap<>();
        addResultsFrom(modId, result);
        return result;
    }

    private static void addResultsFrom(String modId, Map<String, String> result) {
        ModDatabase.ModLatestVersion mod = ModDatabase.listLatestVersions(CelesteModSearchService.database).stream()
            .filter(mf -> mf.file().modId.equals(modId))
            .findFirst().orElse(null);

        if (mod == null) {
            // bail out, we're checking a mod that does not exist in the database!
            return;
        }

        result.put(modId,  "https://celestemodupdater.0x0a.de/banana-mirror/" + mod.file().mirrorName + ".zip");

        for (DependencyRecord dependency : mod.file().dependencies) {
            String dependencyName = dependency.name;

            if (!result.containsKey(dependencyName)) {
                // we need to add this mod and its own dependencies to the list.
                addResultsFrom(dependencyName, result);
            }
        }
    }
}
