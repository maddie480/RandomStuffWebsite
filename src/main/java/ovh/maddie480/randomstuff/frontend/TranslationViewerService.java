package ovh.maddie480.randomstuff.frontend;

import com.google.common.collect.ImmutableMap;
import io.github.everestapi.DialogFileTranslationEditor;
import io.github.everestapi.EverestTranslationEditor;
import io.github.everestapi.OlympusTranslationEditor;
import io.github.everestapi.TranslationEditor;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ovh.maddie480.randomstuff.frontend.UnhandledExceptionFilter.sendDiscordMessage;

@WebServlet(name = "TranslationViewerService", loadOnStartup = 13, urlPatterns = {
        "/celeste/translation-viewer", "/celeste/translation-viewer-reload"})
public class TranslationViewerService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TranslationViewerService.class);

    private static final Map<String, Map<String, Map<String, String>>> dialogs = new HashMap<>();

    @Override
    public void init() {
        refresh();

        try {
            sendDiscordMessage("Frontend Service", ":arrow_up: :globe_with_meridians: The frontend just started.");
        } catch (IOException e) {
            log.warn("Sending startup notification failed!", e);
        }
    }

    private record Repo(String orgName, String repoName, String branch, String dir,
                        String id, Supplier<TranslationEditor> editor) {
    }

    private void refresh() {
        try {
            Map<String, Map<String, Map<String, String>>> newDialogs = new HashMap<>();

            for (Repo r : Arrays.asList(
                    new Repo("EverestAPI", "Everest", "dev",
                            "Celeste.Mod.mm/Content/Dialog", "everest",
                            EverestTranslationEditor::getTranslationEditor),
                    new Repo("EverestAPI", "CelesteCollabUtils2", "master",
                            "Dialog", "cu2", DialogFileTranslationEditor::getTranslationEditor),
                    new Repo("maddie480", "ExtendedVariantMode", "master",
                            "Dialog", "evm", DialogFileTranslationEditor::getTranslationEditor)
            )) {
                GitOperator.cloneRepository(r.orgName, r.repoName, r.branch);
                Path baseDir = Paths.get("/tmp/" + r.repoName + "_repo");
                List<String> languages;
                try (Stream<Path> paths = Files.list(baseDir.resolve(r.dir))) {
                    languages = paths
                            .filter(p -> p.getFileName().toString().endsWith(".txt"))
                            .map(p -> p.getFileName().toString())
                            .map(p -> p.substring(0, p.length() - 4))
                            .toList();
                }
                Map<String, Map<String, String>> newEverestDialog = languages.stream()
                        .collect(Collectors.toMap(l -> l, l -> {
                            try {
                                return r.editor.get().readLanguageEntries(baseDir, l);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                newDialogs.put(r.id, newEverestDialog);
                FileUtils.deleteDirectory(baseDir.toFile());
            }
            {
                GitOperator.cloneRepository("EverestAPI", "Olympus", "main");
                Path baseDir = Paths.get("/tmp/Olympus_repo");
                List<String> languages;
                try (Stream<Path> paths = Files.list(baseDir.resolve("src/lang"))) {
                    languages = paths
                            .filter(p -> p.getFileName().toString().endsWith(".lua"))
                            .map(p -> p.getFileName().toString())
                            .map(p -> p.substring(0, p.length() - 4))
                            .toList();
                }
                Map<String, Map<String, String>> newOlympusDialog = languages.stream()
                        .collect(Collectors.toMap(l -> l, l -> {
                            try {
                                return OlympusTranslationEditor.getTranslationEditor().readLanguageEntries(baseDir, l);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                newDialogs.put("olympus", newOlympusDialog);
                FileUtils.deleteDirectory(baseDir.toFile());
            }

            dialogs.clear();
            dialogs.putAll(newDialogs);

            log.info("Refreshed dialog keys successfully, language count per program: {}", dialogs.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size())));
        } catch (IOException e) {
            log.warn("Loading dialog keys and values failed!", e);
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/translation-viewer-reload")) {
            if (("key=" + SecretConstants.RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refresh();
            } else {
                // invalid secret
                log.warn("Invalid key");
                response.setStatus(403);
            }
            return;
        }

        String program = request.getParameter("program");
        String language = request.getParameter("language");
        boolean redirect = false;

        if (program == null || !dialogs.containsKey(program)) {
            program = "everest";
            redirect = true;
        }

        Map<String, Map<String, String>> dialog = dialogs.get(program);
        List<String> availableLanguages = new ArrayList<>(dialog.keySet());
        availableLanguages.sort(Comparator.naturalOrder());
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

        String programName = ImmutableMap.of(
                "everest", "Everest",
                "olympus", "Olympus",
                "cu2", "Collab Utils 2",
                "evm", "Extended Variant Mode"
        ).get(program);

        String pageTitle = programName + " Translation Viewer";
        request.setAttribute("programName", programName);
        request.setAttribute("title", pageTitle);
        request.setAttribute("leftLang", leftLang);
        request.setAttribute("rightLang", language);
        request.setAttribute("langs", availableLanguages);
        request.setAttribute("entries", dialogEntries);

        PageRenderer.render(request, response, "translation-viewer", pageTitle,
                "View the translations of " + programName + " in different languages, and compare" +
                        " them to English to spot outdated and missing translations.");
    }
}
