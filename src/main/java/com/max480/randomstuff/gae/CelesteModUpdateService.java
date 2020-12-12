package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "CelesteModUpdateService", loadOnStartup = 1, urlPatterns = {"/celeste/everest_update.yaml"})
public class CelesteModUpdateService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModUpdateService");

    @Override
    public void init() {
        try {
            logger.log(Level.INFO, "Warmup: Main server everest_update.yaml is " +
                    IOUtils.toByteArray(getConnectionWithTimeouts(Constants.MAIN_SERVER_URL)).length + " bytes long");
            logger.log(Level.INFO, "Warmup: Backup server everest_update.yaml is " +
                    IOUtils.toByteArray(getConnectionWithTimeouts(Constants.BACKUP_SERVER_URL)).length + " bytes long");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "text/yaml");
        try {
            byte[] yaml = IOUtils.toByteArray(getConnectionWithTimeouts(Constants.MAIN_SERVER_URL));
            IOUtils.write(yaml, response.getOutputStream());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Main server unreachable, falling back to backup server: " + e.toString());
            byte[] yaml = IOUtils.toByteArray(getConnectionWithTimeouts(Constants.BACKUP_SERVER_URL));
            IOUtils.write(yaml, response.getOutputStream());
        }
    }

    public static URLConnection getConnectionWithTimeouts(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(10000);
        return connection;
    }
}
