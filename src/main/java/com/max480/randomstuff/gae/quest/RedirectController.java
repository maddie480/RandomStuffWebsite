package com.max480.randomstuff.gae.quest;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "RedirectController", urlPatterns = {"/quest/discord"})
public class RedirectController extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(302);
        response.setHeader("Location", "https://discord.gg/9eVHrxs");
    }
}
