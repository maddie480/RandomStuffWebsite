package com.max480.randomstuff.gae;


import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Just a simple test to see how IFTTT goes when it comes to reposting tweets to a webhook.
 */
@WebServlet(name = "TwitterIFTTTTest", urlPatterns = {"/twitter-ifttt-test"})
public class TwitterIFTTTTest extends HttpServlet {
    private static final Logger logger = Logger.getLogger("TwitterIFTTTTest");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        logger.info("Request body: " + IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8));
    }
}
