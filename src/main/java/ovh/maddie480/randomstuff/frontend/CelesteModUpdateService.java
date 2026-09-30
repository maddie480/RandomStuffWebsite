package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This servlet provides the everest_update.yaml Everest downloads to check for updates.
 */
@WebServlet(name = "CelesteModUpdateService", urlPatterns = {
        "/celeste/everest_update.yaml", "/celeste/mod_search_database.yaml", "/celeste/mod_files_database.zip",
        "/celeste/mod_dependency_graph.yaml", "/celeste/mod_database.yaml"})
public class CelesteModUpdateService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteModUpdateService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setHeader("Content-Type", request.getRequestURI().endsWith(".zip") ? "application/zip" : "text/yaml");
        String target = switch (request.getRequestURI()) {
            case "/celeste/everest_update.yaml" -> "/shared/celeste/updater/everest-update.yaml";
            case "/celeste/mod_search_database.yaml" -> "/shared/celeste/updater/mod-search-database.yaml";
            case "/celeste/mod_files_database.zip" -> "/shared/celeste/updater/mod-files-database.zip";
            case "/celeste/mod_dependency_graph.yaml" -> "/shared/celeste/updater/mod-dependency-graph.yaml";
            case "/celeste/mod_database.yaml" -> "/shared/celeste/mod-database.yaml";
            default -> null;
        };

        if (target == null) {
            // this should never happen, all URLs handled by the servlet are in the switch case above.
            log.warn("Not found");
            response.setStatus(404);
            PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                    "Oops, this link seems invalid. Please try again!");
            return;
        }

        CacheAndCompressionFilter.setUpForDirectFileSend(response, target);
    }
}
