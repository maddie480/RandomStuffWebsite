package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Tries catching meanies that cause a number of 4xx errors and temp-banning them.
 * ... well that's the idea anyway, for now it just complains in the logs.
 */
public class TheMeanBeanMachine extends HttpFilter {
    private static final Logger log = LoggerFactory.getLogger(TheMeanBeanMachine.class);

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(req, res);

        if (res.getStatus() / 100 == 4) {
            log.info("User {} had a request with status code {}", req.getRemoteAddr(), res.getStatus());
        }
    }
}
