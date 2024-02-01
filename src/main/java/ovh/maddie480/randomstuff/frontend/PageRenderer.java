package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class PageRenderer {
    public static void render(HttpServletRequest request, HttpServletResponse response, String pageId, String title, String description)
            throws ServletException, IOException {
        render(request, response, pageId, pageId, title, description, false, 0);
    }

    public static void render(HttpServletRequest request, HttpServletResponse response, String pageId, String title, String description, int refreshAfter)
            throws ServletException, IOException {
        render(request, response, pageId, pageId, title, description, false, refreshAfter);
    }

    public static void render(HttpServletRequest request, HttpServletResponse response, String pageId, String navId, String title, String description, int refreshAfter)
            throws ServletException, IOException {
        render(request, response, pageId, navId, title, description, false, refreshAfter);
    }

    public static void render(HttpServletRequest request, HttpServletResponse response, String pageId, String navId, String title, String description)
            throws ServletException, IOException {
        render(request, response, pageId, navId, title, description, false, 0);
    }

    public static void render(HttpServletRequest request, HttpServletResponse response, String pageId, String title, String description, boolean includeDownloadJS)
            throws ServletException, IOException {
        render(request, response, pageId, pageId, title, description, includeDownloadJS, 0);
    }

    public static void render(HttpServletRequest request, HttpServletResponse response, String pageId, String navId, String title, String description, boolean includeDownloadJS, int refreshAfter)
            throws ServletException, IOException {

        request.setAttribute("pageId", pageId);
        request.setAttribute("navId", navId);
        request.setAttribute("pageTitle", title);
        request.setAttribute("pageDescription", description);
        request.setAttribute("includeDownloadJS", includeDownloadJS);
        request.setAttribute("refreshAfter", refreshAfter);
        request.setAttribute("isCeleste", request.getRequestURI().startsWith("/celeste/"));

        request.getRequestDispatcher("/WEB-INF/base.jsp").forward(request, response);
    }
}
