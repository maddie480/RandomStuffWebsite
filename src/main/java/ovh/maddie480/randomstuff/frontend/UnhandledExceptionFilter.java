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
 * A filter that catches unhandled exceptions to display an error page.
 */
public class UnhandledExceptionFilter extends HttpFilter {
    private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionFilter.class);

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(req, res);
            if (res.getStatus() / 100 == 5) shoutAtMaddie(req, res, null);
        } catch (Exception e) {
            // trying to use the type directly yields a "class not found exception", so here we are.
            if ("org.eclipse.jetty.io.EofException".equals(e.getClass().getName())) {
                // "connection reset by peer" kinds of errors, do not log or try to handle those
                return;
            }

            log.error("Uncaught exception happened!", e);

            res.setStatus(500);
            res.setHeader("Content-Security-Policy", "default-src 'self'; " +
                    "script-src 'self' https://cdn.jsdelivr.net; " +
                    "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';");

            PageRenderer.render(req, res, "internal-server-error", "Internal Server Error",
                    "Oops, something bad happened. Try again!");

            shoutAtMaddie(req, null, e);
        }
    }

    private static void shoutAtMaddie(HttpServletRequest req, HttpServletResponse res, Exception e) {
        try {
            String message;
            if (e != null) {
                message = ":scream: Someone crashed URI `" + req.getRequestURI() + "`! Here is what happened: `" + e + "`";
            } else {
                message = ":scream: Someone made a " + res.getStatus() + " error happen on the website! URI: `" + req.getRequestURI() + "`";
            }

            if (message.length() > 2000) message = message.substring(0, 1996) + "...`";
            sendDiscordMessage("Website Explosion Report", message);
        } catch (Exception ex) {
            log.warn("Failed alerting about 5xx error", ex);
        }
    }

    static void sendDiscordMessage(String author, String message) throws IOException {
        log.debug("Preparing to complain to my manager...");

        JSONObject o = new JSONObject();
        o.put("avatar_url", "https://raw.githubusercontent.com/maddie480/RandomBackendStuff/main/webhook-avatars/compute-engine.png");
        o.put("username", author);
        o.put("content", message);

        // disallow mentions
        JSONObject allowedMentions = new JSONObject();
        allowedMentions.put("parse", new JSONArray());
        o.put("allowed_mentions", allowedMentions);

        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(SecretConstants.UNHANDLED_EXCEPTIONS_WEBHOOK_URL);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        // apparently, new threads just vanish, so we're doing this synchronously, better be quick
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(3000);

        try (OutputStream os = connection.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            o.write(writer);
        }

        log.debug("ok I'm done screaming, Discord said: \"{}\"", connection.getResponseCode());
    }
}
