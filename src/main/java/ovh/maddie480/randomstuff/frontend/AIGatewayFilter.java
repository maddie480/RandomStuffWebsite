package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * I fully expect all AI crawlers and agents to follow robots.txt directives. Definitely.
 */
public class AIGatewayFilter extends HttpFilter {
    private static final Logger log = LoggerFactory.getLogger(AIGatewayFilter.class);

    private List<String> userAgents;
    private final AtomicInteger counter = new AtomicInteger(0);

    private void loadUserAgents() throws IOException {
        List<String> result = new ArrayList<>();

        try (InputStream is = StaticAssetsAndRouteNotFoundServlet.class.getClassLoader().getResourceAsStream("resources/robots.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String s;
            while ((s = br.readLine()) != null) {
                if (s.startsWith("User-Agent: ")) {
                    result.add(s.substring("User-Agent: ".length()));
                }
            }
        }

        userAgents = result;
        log.debug("Loaded AI user agents: {}", userAgents);
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (userAgents == null) loadUserAgents();

        if (passesGate(req)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(403);
            res.setContentType("text/plain");
            res.getWriter().write("Stop it.\n");
            log.warn("AI agents ruining everything yet again, episode {} ({})",
                    counter.incrementAndGet(), req.getHeader("User-Agent"));
        }
    }

    private boolean passesGate(HttpServletRequest request) {
        if (request.getRequestURI().equals("/robots.txt")) return true;

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return true;

        // try to avoid the Scunthorpe Problem though
        // return userAgents.stream()
        //        .noneMatch(u -> userAgent.matches(".*([^A-Za-z-_]|^)" + u + "([^A-Za-z-_]|$).*"));
        return true;
    }
}
