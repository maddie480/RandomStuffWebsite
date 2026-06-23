package ovh.maddie480.randomstuff.frontend;

import io.github.everestapi.EverestTranslationEditor;
import io.github.everestapi.OlympusTranslationEditor;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.maddie480.randomstuff.frontend.discord.newspublisher.GitOperator;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ovh.maddie480.randomstuff.frontend.UnhandledExceptionFilter.sendDiscordMessage;

@WebServlet(name = "TranslationViewerService", loadOnStartup = 13, urlPatterns = {"/celeste/translation-viewer"})
public class TranslationViewerService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TranslationViewerService.class);

    private static final Map<String, Map<String, String>> everestDialog = new HashMap<>();
    private static final Map<String, Map<String, String>> olympusDialog = new HashMap<>();

    @Override
    public void init() {
        refresh();

        try {
            sendDiscordMessage("Frontend Service", ":arrow_up: :globe_with_meridians: The frontend just started.");
        } catch (IOException e) {
            log.warn("Sending startup notification failed!", e);
        }
    }

    private void refresh() {
        try {
            {
                GitOperator.cloneRepository("Everest", "dev");
                Path baseDir = Paths.get("/tmp/Everest_repo");
                List<String> languages;
                try (Stream<Path> paths = Files.list(baseDir.resolve("Celeste.Mod.mm/Content/Dialog"))) {
                    languages = paths
                            .filter(p -> p.getFileName().toString().endsWith(".txt"))
                            .map(p -> p.getFileName().toString())
                            .map(p -> p.substring(0, p.length() - 4))
                            .toList();
                }
                Map<String, Map<String, String>> newEverestDialog = languages.stream()
                        .collect(Collectors.toMap(l -> l, l -> {
                            try {
                                return EverestTranslationEditor.getTranslationEditor().readLanguageEntries(baseDir, l);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                everestDialog.clear();
                everestDialog.putAll(newEverestDialog);
                FileUtils.deleteDirectory(baseDir.toFile());
            }
            {
                GitOperator.cloneRepository("Olympus", "main");
                Path baseDir = Paths.get("/tmp/Olympus_repo");
                List<String> languages = new ArrayList<>();
                try (BufferedReader br = Files.newBufferedReader(baseDir.resolve("src/lang.lua"), StandardCharsets.UTF_8)) {
                    String s;
                    while ((s = br.readLine()) != null && !s.startsWith("local langs =")) ;
                    while ((s = br.readLine()) != null && !s.startsWith("}")) {
                        s = s.trim();
                        languages.add(s.substring(0, s.indexOf(" ")));
                    }
                }
                Map<String, Map<String, String>> newOlympusDialog = languages.stream()
                        .collect(Collectors.toMap(l -> l, l -> {
                            try {
                                return OlympusTranslationEditor.getTranslationEditor().readLanguageEntries(baseDir, l);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                olympusDialog.clear();
                olympusDialog.putAll(newOlympusDialog);
                FileUtils.deleteDirectory(baseDir.toFile());
            }

            log.info("Refreshed dialog keys for Everest ({}) and Olympus ({})", everestDialog.keySet(), olympusDialog.keySet());
        } catch (IOException e) {
            log.warn("Loading dialog keys and values failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String program = request.getParameter("program");
        String language = request.getParameter("language");
        boolean redirect = false;

        if (program == null || !Arrays.asList("olympus", "everest").contains(program)) {
            program = "everest";
            redirect = true;
        }

        Map<String, Map<String, String>> dialog = program.equals("everest") ? everestDialog : olympusDialog;
        List<String> availableLanguages = new ArrayList<>(dialog.keySet());
        availableLanguages.remove("en");
        availableLanguages.remove("English");
        if (language == null || !availableLanguages.contains(language)) {
            language = availableLanguages.getFirst();
            redirect = true;
        }

        if (redirect) {
            response.sendRedirect("/celeste/translation-viewer?program=" + program + "&language=" + URLEncoder.encode(language, StandardCharsets.UTF_8));
            return;
        }

        String leftLang = dialog.containsKey("en") ? "en" : "English";
        List<Triple<String, String, String>> dialogEntries = new ArrayList<>();

        LinkedHashSet<String> dialogKeys = new LinkedHashSet<>();
        dialogKeys.addAll(dialog.get(leftLang).keySet());
        dialogKeys.addAll(dialog.get(language).keySet());
        for (String dialogKey : dialogKeys) {
            dialogEntries.add(Triple.ofNonNull(
                    dialogKey,
                    dialog.get(leftLang).getOrDefault(dialogKey, ""),
                    dialog.get(language).getOrDefault(dialogKey, "")
            ));
        }

        String pageTitle = (program.equals("everest") ? "Everest" : "Olympus") + " Translation Viewer";
        request.setAttribute("title", pageTitle);
        request.setAttribute("leftLang", leftLang);
        request.setAttribute("rightLang", language);
        request.setAttribute("langs", availableLanguages);
        request.setAttribute("entries", dialogEntries);

        PageRenderer.render(request, response, "translation-viewer", pageTitle, "wip wip wip");
    }
}
