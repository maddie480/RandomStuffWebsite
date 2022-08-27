package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * This servlet is just here to serve the Vue app.
 * The build script puts all the resources along with the Java app's resources, and moves Vue's index.html
 * into a vue-index.html within the project's classpath. So we just need to send that!
 */
@WebServlet(name = "VueServer", urlPatterns = {"/celeste/wipe-converter", "/celeste/banana-mirror-browser"})
public class VueServer extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try (InputStream is = VueServer.class.getClassLoader().getResourceAsStream("vue-index.html")) {
            IOUtils.copy(is, response.getOutputStream());
        }
    }
}
