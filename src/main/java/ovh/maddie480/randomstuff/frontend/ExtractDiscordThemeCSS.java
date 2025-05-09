package ovh.maddie480.randomstuff.frontend;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtractDiscordThemeCSS {
    private static final Logger logger = Logger.getLogger("ExtractDiscordThemeCSS");

    public static void main(String[] args) throws IOException, InterruptedException {
        Set<String> cssUrls;

        { // extract the CSS locations from Discord once it's done loading on browser

            // chromium is borked on GitHub Actions runners
            String browser = System.getenv("CI") == null ? "chromium" : "google-chrome";
            System.out.println("Launching browser: " + browser);

            Process p = new ProcessBuilder(browser, "--headless", "--dump-dom", "https://discord.com/app")
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start();

            try (InputStream is = p.getInputStream()) {
                cssUrls = Jsoup.parse(is, "UTF-8", "https://discord.com/app")
                        .select("link[rel=\"stylesheet\"]").stream()
                        .map(l -> l.absUrl("href"))
                        .collect(Collectors.toSet());
            }

            p.waitFor();

            if (cssUrls.isEmpty()) throw new IOException("No CSS URL found!");
        }

        Set<String> quickCssClasses;
        try (Stream<String> lines = Files.lines(Paths.get("../../../../src/main/webapp/WEB-INF/classes/resources/static/css/vencord-quick-css.css"))) {
            String linesFiltered = lines
                    .filter(s -> !s.contains("https://"))
                    .collect(Collectors.joining("\n"));

            quickCssClasses = findCssClasses(linesFiltered)
                    .stream().filter(c -> !c.matches("\\.[0-9]+"))
                    .filter(c -> !c.equals(".55em")) // *sigh* this is not a CSS class
                    .collect(Collectors.toSet());
        }

        Set<String> existingCssClasses = new HashSet<>();

        Files.createDirectories(Paths.get("resources/static/css/discord-nitro-themes"));
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("resources/static/css/discord-nitro-themes/common.css"), StandardCharsets.UTF_8)) {
            int i = 0;
            for (String cssUrl : cssUrls) {
                logger.info("Fetching " + cssUrl + " (" + (++i) + "/" + cssUrls.size() + ")");
                String css = IOUtils.toString(ConnectionUtils.openStreamWithTimeout(cssUrl), StandardCharsets.UTF_8);
                existingCssClasses.addAll(findCssClasses(css));
                copyCustomThemeBackgroundCss(css, bw);
            }
        }

        quickCssClasses.removeAll(existingCssClasses);
        if (!quickCssClasses.isEmpty()) throw new IOException("Missing classes in QuickCSS: " + quickCssClasses);
    }

    private static Set<String> findCssClasses(String css) {
        return Pattern.compile("\\.[0-9a-zA-Z_-]+").matcher(css)
                .results()
                .map(MatchResult::group)
                .collect(Collectors.toSet());
    }

    private static void copyCustomThemeBackgroundCss(String css, BufferedWriter out) throws IOException {
        while (true) {
            int startOfComment = css.indexOf("/*");
            if (startOfComment == -1) break;

            int endOfComment = startOfComment + css.substring(startOfComment).indexOf("*/") + 2;
            if (endOfComment == startOfComment + 1) break;

            css = css.substring(0, startOfComment) + css.substring(endOfComment);
        }

        while (css.contains("{")) {
            String classes = css.substring(0, css.indexOf("{"));
            css = css.substring(classes.length());
            StringBuilder contents = new StringBuilder();

            int offset = 0;
            int depth = 0;
            do {
                char c = css.charAt(offset);
                contents.append(c);
                if (c == '{') depth++;
                if (c == '}') depth--;
                offset++;
            } while (depth > 0);

            css = css.substring(offset);

            if (classes.contains(".custom-theme-background")) {
                classes = classes.replace(".custom-theme-background", "");
                if (classes.isBlank()) classes = ":root";
                out.write(classes + contents);
            }
        }
    }
}
