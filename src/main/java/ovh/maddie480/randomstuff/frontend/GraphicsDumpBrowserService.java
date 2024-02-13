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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@WebServlet(name = "GraphicsDumpBrowserService", urlPatterns = {"/celeste/graphics-dump-browser/*"})
public class GraphicsDumpBrowserService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(GraphicsDumpBrowserService.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (ZipInputStream zip = new ZipInputStream(GraphicsDumpBrowserService.class.getClassLoader().getResourceAsStream("resources/static/graphics-dump.zip"))) {
            String searchedFileName = req.getRequestURI().substring(31);

            if (searchedFileName.equals("list.json")) {
                List<String> fileListing = new ArrayList<>();

                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        fileListing.add(entry.getName());
                    }
                }

                log.debug("Responding with file listing, containing {} elements", fileListing.size());
                resp.setContentType("image/png");
                resp.getWriter().write(new JSONArray(fileListing).toString());
                return;
            }

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && searchedFileName.equals(entry.getName())) {
                    log.debug("File found in graphics dump: {}", searchedFileName);
                    resp.setContentType("image/png");
                    IOUtils.copy(zip, resp.getOutputStream());
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
