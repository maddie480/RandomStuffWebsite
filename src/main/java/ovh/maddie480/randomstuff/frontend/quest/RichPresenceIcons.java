package ovh.maddie480.randomstuff.frontend.quest;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * An API that literally responds with four hardcoded CRC32 hashes. Wow.
 */
@WebServlet(name = "RichPresenceIcons", urlPatterns = {"/quest/quest-mod-manager/rich-presence-icons"})
public class RichPresenceIcons extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write("""
                3905829c
                5fda59c0
                f0e7b64c
                f79043d5""");
    }
}
