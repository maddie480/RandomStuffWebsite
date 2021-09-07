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

/**
 * A filter adding a Content-Security-Policy header on all non-static files.
 * It also serves Renogare.otf with a header allowing it to be used on gamebanana.com.
 */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = "/*")
public class SecurityHeadersFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        // allow only self as an origin, except for styles, and prevent the site from being iframed.
        res.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self' 'unsafe-inline' https://stackpath.bootstrapcdn.com; frame-ancestors 'none'; object-src 'none';");
        chain.doFilter(req, res);
    }

    @WebServlet(name = "FontServlet", urlPatterns = "/fonts/Renogare.otf")
    public static class FontServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/font-sfnt");
            resp.setHeader("Access-Control-Allow-Origin", "https://gamebanana.com");
            try (InputStream is = FontServlet.class.getClassLoader().getResourceAsStream("font-generator/fonts/Renogare.otf")) {
                IOUtils.copy(is, resp.getOutputStream());
            }
        }
    }
}
