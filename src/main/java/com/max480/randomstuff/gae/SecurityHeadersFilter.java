package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * A filter adding a Content-Security-Policy header on all non-static files.
 */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = "/*")
public class SecurityHeadersFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        // allow only self as an origin, except for styles, and prevent the site from being iframed (except for the arbitrary mod app settings).
        if (req.getRequestURI().equals("/gamebanana/arbitrary-mod-app-settings")) {
            res.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline' https://stackpath.bootstrapcdn.com; frame-ancestors https://gamebanana.com; object-src 'none';");
        } else if (req.getRequestURI().equals("/parrot-quick-importer-online")) {
            res.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' https://code.jquery.com; img-src 'self' https://cultofthepartyparrot.com; frame-ancestors 'none'; object-src 'none';");
        } else {
            res.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline' https://stackpath.bootstrapcdn.com; frame-ancestors 'none'; object-src 'none';");
        }
        if (req.getRequestURI().equals("/celeste/update-checker-status") && "widget=true".equals(req.getQueryString())) {
            res.setHeader("Access-Control-Allow-Origin", "https://gamebanana.com");
        }
        chain.doFilter(req, res);
    }
}
