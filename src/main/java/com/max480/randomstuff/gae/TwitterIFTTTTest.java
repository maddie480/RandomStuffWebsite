package com.max480.randomstuff.gae;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Just a simple test to see how IFTTT goes when it comes to reposting tweets to a webhook.
 */
@WebServlet(name = "TwitterIFTTTTest", urlPatterns = {"/twitter-ifttt-test"})
public class TwitterIFTTTTest extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TwitterIFTTTTest.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        log.info("Request body: " + IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8));
    }
}
