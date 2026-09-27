package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static ovh.maddie480.randomstuff.frontend.UnhandledExceptionFilter.sendDiscordMessage;

@WebServlet(name = "LoadingDoneNotifier", loadOnStartup = 13)
public class LoadingDoneNotifier extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(LoadingDoneNotifier.class);

    @Override
    public void init() {
        try {
            sendDiscordMessage("Frontend Service", ":arrow_up: :globe_with_meridians: The frontend just started.");
        } catch (IOException e) {
            log.warn("Sending startup notification failed!", e);
        }
    }
}
