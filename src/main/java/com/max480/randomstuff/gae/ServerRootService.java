package com.max480.randomstuff.gae;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ServerRootService", urlPatterns = {""})
public class ServerRootService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // for now there is nothing public but Celeste stuff, so just redirect there.
        response.setStatus(302);
        response.setHeader("Location", "/celeste/");
    }
}
