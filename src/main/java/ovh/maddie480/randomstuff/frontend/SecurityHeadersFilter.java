package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * A filter adding a Content-Security-Policy header on all non-static files.
 */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = "/*")
public class SecurityHeadersFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        // default Content-Security-Policy: only stuff from the website, allow Bootstrap scripts and styles, allow inline styles, disallow iframes and objects.

        if (req.getRequestURI().equals("/gamebanana/arbitrary-mod-app-settings")) {
            // in addition, allow being iframed from GameBanana.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "frame-ancestors https://gamebanana.com; " +
                    "object-src 'none';");
        } else if (Arrays.asList("/celeste/font-generator", "/celeste/direct-link-service",
                "/celeste/collab-contest-editor", "/celeste/collab-contest-list").contains(req.getRequestURI())) {
            // in addition, allow data URLs: Bootstrap dropdowns / checkboxes use inline SVG for their arrow pointing down.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (req.getRequestURI().equals("/celeste/banana-mirror-browser")) {
            // allow getting the images from Banana Mirror.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "img-src 'self' data: https://*.0x0a.de; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (Arrays.asList("/celeste/map-tree-viewer", "/celeste/file-searcher", "/celeste/graphics-dump-browser",
                        "/celeste/asset-drive", "/celeste/wipe-converter")
                .contains(req.getRequestURI())) {

            // default rules for the Vue app
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if ("/celeste/olympus-news".equals(req.getRequestURI())) {
            // allow images from GitHub
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "img-src 'self' data: https://raw.githubusercontent.com; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if ("/static/parrot-quick-importer-online.html".equals(req.getRequestURI())) {
            // :parrot_parrot:
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "img-src 'self' https://cultofthepartyparrot.com; " +
                    "script-src 'self' https://code.jquery.com; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (Arrays.asList("/quest/mods", "/quest/tools", "/quest/backgrounds").contains(req.getRequestURI())) {
            // legacy stuff *has* to use jQuery
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net https://code.jquery.com; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if ("/lnj-emotes".equals(req.getRequestURI())) {
            // the emotes come from Discord, actually
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "img-src 'self' https://cdn.discordapp.com; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if ("/static/get-mod-dl-links-with-gbapi.html".equals(req.getRequestURI())) {
            // this one is pretty weird... it only needs access to jQuery and GameBanana.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://code.jquery.com; " +
                    "connect-src 'self' https://gamebanana.com; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else {
            // default rules
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        }

        if (Arrays.asList("/celeste/update-checker-status", "/fonts/Renogare.otf").contains(req.getRequestURI())) {
            // allow those to be accessed from GameBanana.
            res.setHeader("Access-Control-Allow-Origin", "https://gamebanana.com");
        }
        if ("/fonts/VT323-Regular.ttf".equals(req.getRequestURI())) {
            res.setHeader("Access-Control-Allow-Origin", "http://live-chat.maddie480.ovh:11587");
        }
        if (Arrays.asList("/celeste/gamebanana-search", "/celeste/gamebanana-list", "/celeste/gamebanana-featured", "/celeste/gamebanana-categories",
                        "/celeste/gamebanana-info", "/celeste/bin-to-json", "/celeste/custom-entity-catalog.json", "/celeste/everest-versions",
                        "/celeste/update-checker-status.json", "/celeste/everest_update.yaml", "/celeste/mod_search_database.yaml",
                        "/celeste/mod_dependency_graph.yaml", "/celeste/olympus-news.json", "/celeste/graphics-dump-browser/list.json",
                        "/celeste/olympus-versions")
                .contains(req.getRequestURI())) {
            // allow most JSON and YAML APIs to be called from anywhere.
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Expose-Headers", "X-Total-Count");
        }

        chain.doFilter(req, res);
    }
}
