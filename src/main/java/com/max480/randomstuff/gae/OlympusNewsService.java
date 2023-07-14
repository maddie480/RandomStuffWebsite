package com.max480.randomstuff.gae;

import com.max480.randomstuff.gae.discord.newspublisher.GitOperator;
import com.max480.randomstuff.gae.discord.newspublisher.OlympusNews;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

/**
 *
 */
@WebServlet(name = "OlympusNewsService", urlPatterns = {"/celeste/olympus-news"})
public class OlympusNewsService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(OlympusNewsService.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int page = 1;

        if (request.getQueryString() != null && request.getQueryString().startsWith("page=")) {
            try {
                page = Integer.parseInt(request.getQueryString().substring(5));

                if (page < 1) {
                    log.warn("Page number is out of bounds: {}", page);
                    page = 1;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid number in query string: {}", request.getQueryString(), e);
            }
        }

        List<OlympusNews> news = GitOperator.listOlympusNews();
        news.addAll(GitOperator.listArchivedOlympusNews());

        request.setAttribute("page", page);
        request.setAttribute("pageCount", ((news.size() - 1) / 20) + 1);
        request.setAttribute("news", news.stream()
                .sorted(Comparator.comparing(OlympusNews::slug).reversed())
                .skip((page - 1) * 20L).limit(20)
                .map(entry -> {
                    if (entry.image() == null || !entry.image().startsWith("./")) {
                        return entry;
                    }

                    // resolve relative paths to the corresponding GitHub-hosted images
                    String imageLink = null;
                    if (Files.exists(Paths.get("/tmp/olympus_news_repo/olympusnews").resolve(entry.image()))) {
                        imageLink = "https://raw.githubusercontent.com/EverestAPI/EverestAPI.github.io/main/olympusnews/" + entry.image().substring(2);
                    }
                    if (Files.exists(Paths.get("/tmp/olympus_news_repo/olympusnews/archive").resolve(entry.image()))) {
                        imageLink = "https://raw.githubusercontent.com/EverestAPI/EverestAPI.github.io/main/olympusnews/archive/" + entry.image().substring(2);
                    }

                    return new OlympusNews(entry.slug(), entry.title(),
                            imageLink, entry.link(), entry.shortDescription(), entry.longDescription());
                })
                .toList());

        PageRenderer.render(request, response, "olympus-news", "Olympus News",
                "Find all the news that appeared in Olympus, the mod manager for Celeste, on this page!");
    }
}
