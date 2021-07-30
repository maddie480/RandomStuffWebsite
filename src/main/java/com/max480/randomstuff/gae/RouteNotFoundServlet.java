package com.max480.randomstuff.gae;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RouteNotFound", urlPatterns = {"/"})
public class RouteNotFoundServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/") || request.getRequestURI().equals("/celeste")) {
            // redirect / and /celeste to /celeste/ because there is nothing but Celeste stuff right now anyway.
            response.setStatus(302);
            response.setHeader("Location", "/celeste/");
        } else {
            // display a simple 404 page
            response.setStatus(404);
            response.setHeader("Content-Type", "text/html; charset=UTF-8");
            response.getWriter().write("<html><style>body { font-family: sans-serif; text-align: center; }</style>" +
                    "<h1>\u274C Not Found</h1><a href=\"/\">\u2B05 Back to Home Page</a></html>");
        }
    }
}
