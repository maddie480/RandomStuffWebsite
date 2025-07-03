package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static ovh.maddie480.randomstuff.frontend.UnhandledExceptionFilter.sendDiscordMessage;

/**
 * Tries catching users that cause a number of 4xx errors (brute-forcing some key? fuzzing to find vulnerabilities?)
 * and temp-banning them.
 */
public class TheMeanBeanMachine extends HttpFilter {
    private static final Logger log = LoggerFactory.getLogger(TheMeanBeanMachine.class);
    private static final Path meanBeanFolder = Paths.get("/shared/temp/mean-bean-machine");

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (Files.exists(meanBeanFolder.resolve(req.getRemoteAddr()))) {
            // you've been mean and now you're beaned
            res.setStatus(429);
            res.setContentType("text/plain");
            res.getWriter().write("Too many of your requests ended up in client errors (4xx). Try again later!");
            return;
        }

        chain.doFilter(req, res);

        if (res.getStatus() / 100 == 4) {
            Files.createFile(meanBeanFolder.resolve(req.getRemoteAddr() + "_" + System.currentTimeMillis()));
            long strikes;
            try (Stream<Path> files = Files.list(meanBeanFolder)) {
                strikes = files.filter(f -> f.getFileName().toString().startsWith(req.getRemoteAddr() + "_")).count();
            }
            log.debug("User {} had a request with status code {} (strike #{})", req.getRemoteAddr(), res.getStatus(), strikes);
            if (strikes >= 100) {
                log.warn("User {} has been beaned!", req.getRemoteAddr());
                Files.createFile(meanBeanFolder.resolve(req.getRemoteAddr()));
                shoutAtMaddie();
            }
        }
    }

    private static void shoutAtMaddie(HttpServletRequest req) {
        try {
            sendDiscordMessage("The Mean Bean Machine", "Hey :wave: I just banned `" + req.getRemoteAddr() + "`! k bye :person_walking:");
        } catch (Exception ex) {
            log.warn("Failed alerting about ban", ex);
        }
    }
}
