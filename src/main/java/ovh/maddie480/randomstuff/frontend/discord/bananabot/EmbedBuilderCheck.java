package ovh.maddie480.randomstuff.frontend.discord.bananabot;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.frontend.SecretConstants;

import java.io.IOException;

/**
 * This is the API that makes the BananaBot run.
 */
@WebServlet(name = "EmbedBuilderCheck", urlPatterns = {"/discord/bananabot-embed-builder-check"})
public class EmbedBuilderCheck extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(EmbedBuilderCheck.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
            EmbedBuilder.integrityCheck();
        } else {
            // invalid secret
            log.warn("Invalid key");
            response.setStatus(403);
        }
    }
}
