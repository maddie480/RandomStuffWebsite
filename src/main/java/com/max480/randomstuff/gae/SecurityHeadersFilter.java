package com.max480.randomstuff.gae;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        } else if (req.getRequestURI().equals("/celeste/font-generator")) {
            // in addition, allow data URLs: Bootstrap dropdowns use inline SVG for their arrow pointing down.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (req.getRequestURI().equals("/parrot-quick-importer-online")) {
            // this one has its own separate rules: it was initially from a different app. :p
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "script-src 'self' https://code.jquery.com; " +
                    "img-src 'self' https://cultofthepartyparrot.com; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (req.getRequestURI().equals("/celeste/wipe-converter")) {
            // web worker magic forces us to allow inline JS... ouch.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' 'unsafe-eval' blob:; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (req.getRequestURI().equals("/celeste/banana-mirror-browser")) {
            // allow getting the images from Banana Mirror.
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "img-src 'self' https://celestemodupdater.0x0a.de; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");
        } else if (req.getRequestURI().equals("/celeste/map-tree-viewer")) {
            // allow displaying checkboxes that use SVG as data URLs
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "img-src 'self' data:; " +
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
        if (Arrays.asList("/celeste/gamebanana-search", "/celeste/gamebanana-list", "/celeste/gamebanana-featured", "/celeste/gamebanana-categories")
                .contains(req.getRequestURI())) {
            // allow GameBanana APIs to be called from anywhere.
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Expose-Headers", "X-Total-Count");
        }
        if (req.getRequestURI().equals("/celeste/everest_update.yaml")) {
            // this one can be accessed from the Banana Mirror Browser mirror (... yeah, a mirror for a browser for a mirror, right).
            res.setHeader("Access-Control-Allow-Origin", "https://max4805.github.io");
        }
        if (req.getRequestURI().equals("/celeste/everest-versions")) {
            // this one can be accessed from the Everest website to determine the link to the latest version of Everest.
            res.setHeader("Access-Control-Allow-Origin", "https://everestapi.github.io");
        }

        chain.doFilter(req, res);
    }
}
