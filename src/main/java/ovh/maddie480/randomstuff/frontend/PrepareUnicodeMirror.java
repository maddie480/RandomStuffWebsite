package ovh.maddie480.randomstuff.frontend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ovh.maddie480.randomstuff.frontend.PrepareForRadioLNJ.copyRecursive;

/**
 * The official Unicode emoji lists take forever to load (10 KB/s bandwidth limit past the 1st MB? WTF?)
 * and a script is MUCH more patient than a user...
 */
public class PrepareUnicodeMirror {
    private static final Path targetDirectory = Paths.get("resources/static/unicode-mirror");

    public static void main(String[] args) throws IOException, InterruptedException {
        Path cachedUnicodeMirror = Paths.get("/tmp/old-target/random-stuff-website-1.0.0/WEB-INF/classes/resources/static/unicode-mirror");
        if (Files.isDirectory(cachedUnicodeMirror)) {
            copyRecursive(cachedUnicodeMirror, targetDirectory);
            return;
        }

        wget();
        Files.move(targetDirectory.getParent().resolve("www.unicode.org"), targetDirectory);
    }

    private static void wget() throws InterruptedException, IOException {
        boolean fastMode = (System.getenv("RADIO_LNJ_SOURCES") == null);

        int exitCode = new ProcessBuilder(
                "wget",
                "-pk" + (fastMode ? "" : "r"), // page requisites + convert links + recursive
                "-np", // do not go up to the rest of the Unicode website
                "--tries=3", "--retry-on-host-error", "--retry-connrefused", // be tolerant of errors
                "https://www.unicode.org/emoji/charts/" + (fastMode ? "emoji-released.html" : "")
        )
                .directory(targetDirectory.getParent().toFile())
                .inheritIO()
                .start()
                .waitFor();

        if (exitCode != 0) throw new IOException("wget exited with code " + exitCode);
    }
}
