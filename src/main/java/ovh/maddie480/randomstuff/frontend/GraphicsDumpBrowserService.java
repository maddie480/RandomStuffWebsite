package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@WebServlet(name = "GraphicsDumpBrowserService", urlPatterns = {"/celeste/graphics-dump-browser/*"}, loadOnStartup = 12)
public class GraphicsDumpBrowserService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(GraphicsDumpBrowserService.class);

    @Override
    public void init() {
        try (InputStream is = GraphicsDumpBrowserService.class.getClassLoader().getResourceAsStream("resources/static/graphics-dump.zip");
            OutputStream os = Files.newOutputStream(Paths.get("/tmp/graphics-dump.zip"))) {

            IOUtils.copy(is, os);

        } catch (IOException e) {
            log.warn("Preparing graphics dump browser failed!", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (ZipFile zip = new ZipFile("/tmp/graphics-dump.zip")) {
            String searchedFileName = req.getRequestURI().substring(31);

            if (searchedFileName.equals("list.json")) {
                List<String> fileListing = new ArrayList<>();
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        fileListing.add(entry.getName());
                    }
                }

                log.debug("Responding with file listing, containing {} elements", fileListing.size());
                resp.setContentType("application/json");
                resp.getWriter().write(new JSONArray(fileListing).toString());
                return;
            }

            ZipEntry entry = zip.getEntry(searchedFileName);
            if (entry != null && !entry.isDirectory()) {
                try (InputStream is = zip.getInputStream(entry)) {
                    log.debug("File found in graphics dump: {}", searchedFileName);
                    resp.setContentType("image/png");
                    IOUtils.copy(is, resp.getOutputStream());
                    return;
                }
            }

            log.warn("File not found in graphics dump: {}", searchedFileName);
            resp.setStatus(404);
            PageRenderer.render(req, resp, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
        }
    }
}
