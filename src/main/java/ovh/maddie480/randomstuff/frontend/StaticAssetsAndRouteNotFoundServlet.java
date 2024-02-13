package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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

    static {
        CONTENT_TYPES = new HashMap<>();
        CONTENT_TYPES.put("css", "text/css");
        CONTENT_TYPES.put("otf", "font/otf");
        CONTENT_TYPES.put("ttf", "font/ttf");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("ico", "image/x-icon");
        CONTENT_TYPES.put("svg", "image/svg+xml");
        CONTENT_TYPES.put("js", "application/javascript");
        CONTENT_TYPES.put("webm", "video/webm");
        CONTENT_TYPES.put("html", "text/html");
        CONTENT_TYPES.put("json", "application/json");
        CONTENT_TYPES.put("mp3", "audio/mpeg");
        CONTENT_TYPES.put("zip", "application/zip");
        CONTENT_TYPES.put("exe", "application/vnd.microsoft.portable-executable");
        CONTENT_TYPES.put("jar", "application/java-archive");
        CONTENT_TYPES.put("csv", "text/csv");
    }

    private static final Logger log = LoggerFactory.getLogger(StaticAssetsAndRouteNotFoundServlet.class);

    private static final Pattern rangePattern = Pattern.compile("^bytes=([0-9]*)-([0-9]*)$");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/")) {
            HomepageService.doGet(request, response);
        } else {
            String extension = request.getRequestURI().substring(request.getRequestURI().lastIndexOf(".") + 1);
            if (CONTENT_TYPES.containsKey(extension) && Stream.of("/css/", "/fonts/", "/img/", "/js/", "/vids/", "/music/", "/quest/", "/lua-cutscenes-documentation/", "/static/")
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
