package com.max480.randomstuff.gae;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Static pages, but that are still JSPs so that they can benefit from base.jsp.
 */
@WebServlet(name = "StaticPagesService", urlPatterns = {"/discord-bots/timezone-bot/detect-timezone",
        "/discord-bots/timezone-bot/timezone-dropdown-help", "/discord-bots/terms-and-privacy"})
public class StaticPagesService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(StaticPagesService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        switch (request.getRequestURI()) {
            case "/discord-bots/timezone-bot/detect-timezone" ->
                    PageRenderer.render(request, response, "detect-timezone", "discord-bots", "Timezone Bot – Timezone Detector",
                            "Use this page to figure out your timezone, for usage with the Timezone Bot.");
            case "/discord-bots/timezone-bot/timezone-dropdown-help" ->
                    PageRenderer.render(request, response, "timezone-dropdown-help", "discord-bots", "Timezone Bot – /timezone-dropdown help",
                            "Check this page if you need help with using Timezone Bot's /timezone-dropdown command.");
            case "/discord-bots/terms-and-privacy" ->
                    PageRenderer.render(request, response, "terms-and-privacy", "discord-bots", "Discord Bots – Terms and Privacy",
                            "This page details the Terms of Service and Privacy Policy of max480's Discord bots, and gives details on which data is stored.");
            default -> {
                log.warn("Not found");
                response.setStatus(404);
                PageRenderer.render(request, response, "page-not-found", "Page Not Found",
                        "Oops, this link seems invalid. Please try again!");
            }
        }
    }
}
