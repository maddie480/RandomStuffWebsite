package ovh.maddie480.randomstuff.frontend;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The default servlet catching all pages that didn't match any other route
 * (because yes, that's what "/" does... it isn't just the server root.)
 * Returns static assets if they exist, or returns a "page not found" error if it does not exist.
 */
@WebServlet(name = "StaticAssetsAndRouteNotFound", urlPatterns = {"/"})
public class StaticAssetsAndRouteNotFoundServlet extends HttpServlet {
    public static final Map<String, String> CONTENT_TYPES;
    public static final Set<String> FORMATS_NOT_WORTH_COMPRESSING;

    // XMLs sometimes don't show up properly in browsers (reportedly due to some extensions),
    // and users probably want to download them anyway, so force download with a Content-Disposition header
    public static final Map<String, String> PATHS_TO_FORCE_DOWNLOAD = ImmutableMap.of(
        "/resources/foregroundtiles.xml", "ForegroundTiles.xml",
        "/resources/celeste_dialogs.xml", "Celeste_Dialogs.xml"
    );

    static {
        // (extension, MIME type, should we compress this?)
        List<Triple<String, String, Boolean>> contentTypesList = new ArrayList<>();
        contentTypesList.add(Triple.of("css", "text/css", true));
        contentTypesList.add(Triple.of("otf", "font/otf", true));
        contentTypesList.add(Triple.of("ttf", "font/ttf", true));
        contentTypesList.add(Triple.of("png", "image/png", false));
        contentTypesList.add(Triple.of("gif", "image/gif", false));
        contentTypesList.add(Triple.of("jpg", "image/jpeg", false));
        contentTypesList.add(Triple.of("ico", "image/x-icon", false));
        contentTypesList.add(Triple.of("svg", "image/svg+xml", true));
        contentTypesList.add(Triple.of("js", "application/javascript", true));
        contentTypesList.add(Triple.of("webm", "video/webm", false));
        contentTypesList.add(Triple.of("html", "text/html", true));
        contentTypesList.add(Triple.of("json", "application/json", true));
        contentTypesList.add(Triple.of("mp3", "audio/mpeg", false));
        contentTypesList.add(Triple.of("zip", "application/zip", false));
        contentTypesList.add(Triple.of("exe", "application/vnd.microsoft.portable-executable", false));
        contentTypesList.add(Triple.of("jar", "application/java-archive", false));
        contentTypesList.add(Triple.of("csv", "text/csv", true));
        contentTypesList.add(Triple.of("xml", "application/xml", true));
        contentTypesList.add(Triple.of("bmfc", "application/octet-stream", true));
        contentTypesList.add(Triple.of("obj", "application/octet-stream", true));
        contentTypesList.add(Triple.of("txt", "text/plain", true));
        contentTypesList.add(Triple.of("webp", "image/webp", false));

        CONTENT_TYPES = new HashMap<>();
        for (Triple<String, String, Boolean> contentType : contentTypesList) {
            CONTENT_TYPES.put(contentType.getLeft(), contentType.getMiddle());
        }
        FORMATS_NOT_WORTH_COMPRESSING = new HashSet<>();
        for (Triple<String, String, Boolean> contentType : contentTypesList) {
            if (!contentType.getRight()) {
                FORMATS_NOT_WORTH_COMPRESSING.add("." + contentType.getLeft());
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StaticAssetsAndRouteNotFoundServlet.class);

    private static final Pattern rangePattern = Pattern.compile("^bytes=([0-9]*)-([0-9]*)$");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/")) {
            HomepageService.doGet(request, response);
        } else {
            String extension = request.getRequestURI().substring(request.getRequestURI().lastIndexOf(".") + 1);
            if (CONTENT_TYPES.containsKey(extension) && Stream.of("/assets/", "/css/", "/fonts/", "/img/", "/js/", "/vids/", "/music/", "/quest/", "/lua-cutscenes-documentation/", "/static/", "/resources/")
                    .anyMatch(request.getRequestURI()::startsWith)) {

                long size;
                try (InputStream is = StaticAssetsAndRouteNotFoundServlet.class.getClassLoader().getResourceAsStream("resources" + request.getRequestURI().replace("%20", " "))) {
                    if (is == null) {
                        display404(request, response);
                        return;
                    }

                    size = IOUtils.consume(is);
                }

                long start = 0;
                long end = size - 1;

                if (request.getHeader("Range") != null) {
                    Matcher rangeMatched = rangePattern.matcher(request.getHeader("Range"));

                    if (!rangeMatched.matches()) {
                        response.setStatus(416);
                        return;
                    }

                    if (!rangeMatched.group(1).isEmpty()) {
                        start = Long.parseLong(rangeMatched.group(1));
                    }

                    if (!rangeMatched.group(2).isEmpty()) {
                        long newEnd = Long.parseLong(rangeMatched.group(2));

                        if (newEnd > end) {
                            // requested bytes past the end of the file!
                            response.setStatus(416);
                            return;
                        }

                        if (rangeMatched.group(1).isEmpty()) {
                            // X bytes from the end
                            start = size - newEnd;
                        } else {
                            end = newEnd;
                        }
                    }

                    response.setStatus(206);
                    response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + size);
                    response.setContentLength((int) (end - start + 1));
                } else {
                    response.setContentLength((int) size);
                }

                response.setHeader("Accept-Ranges", "bytes");
                response.setContentType(CONTENT_TYPES.get(extension));

                if (PATHS_TO_FORCE_DOWNLOAD.containsKey(request.getRequestURI())) {
                    response.setHeader("Content-Disposition", "attachment; filename=\""
                        + PATHS_TO_FORCE_DOWNLOAD.get(request.getRequestURI()) + '"');
                }

                try (InputStream is = StaticAssetsAndRouteNotFoundServlet.class.getClassLoader().getResourceAsStream("resources" + request.getRequestURI().replace("%20", " "))) {
                    IOUtils.copyLarge(is, response.getOutputStream(), start, end - start + 1);
                    return;
                }
            }

            display404(request, response);
        }
    }

    private static void display404(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(404);
        PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
        log.warn("Route not found: {}", request.getRequestURI());
    }
}
