package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * When the Mod Structure Verifier finds issues with a mod zip, it sends a link to this page with parameters
 * corresponding to the bot settings and to the issues it found, to give more help to the user.
 */
@WebServlet(name = "ModStructureVerifierHelp", urlPatterns = {"/celeste/mod-structure-verifier-help"})
@MultipartConfig
public class ModStructureVerifierHelpService extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        PageRenderer.render(request, response, "mod-structure-verifier-help", "mod-structure-verifier",
                "Mod Structure Verifier Help", "Do you have mod structure issues? Here are some steps to solve them.");
    }
}
