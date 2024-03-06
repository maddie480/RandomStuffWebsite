package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A filter that catches unhandled exceptions to display an error page.
 */
@WebFilter(filterName = "UnhandledExceptionFilter", urlPatterns = "/*")
public class UnhandledExceptionFilter extends HttpFilter {
    private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionFilter.class);

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(req, res);
        } catch (Exception e) {
            // trying to use the type directly yields a "class not found exception", so here we are.
            if ("org.eclipse.jetty.io.EofException".equals(e.getClass().getName())) {
                // "connection reset by peer" kinds of errors, do not log or try to handle those
                return;
            }

            log.error("Uncaught exception happened!", e);

            res.setStatus(500);
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");

            PageRenderer.render(req, res, "internal-server-error", "Internal Server Error",
                    "Oops, something bad happened. Try again!");
        }
    }
}
