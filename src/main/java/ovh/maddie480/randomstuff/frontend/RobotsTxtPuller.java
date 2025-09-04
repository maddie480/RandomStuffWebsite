package ovh.maddie480.randomstuff.frontend;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RobotsTxtPuller {
    public static void main(String[] args) throws IOException {
        String robots = "# Keep AI crawlers away from the asset drive,\n" +
                "# as most artists would probably object to this.\n\n" +
                (Jsoup.connect("https://robotstxt.com/ai").get()
                        .select("pre.enable-copy").first().text() + "\n")
                        .replace("/\n", "/celeste/asset-drive\n")
                        .replace("‑" /* non-breaking hyphen */, "-" /* regular hyphen */);

        if (!robots.contains("Disallow: ")) {
            throw new IOException("robots.txt doesn't look right:\n" + robots);
        }

        Files.writeString(Paths.get("resources/robots.txt"), robots, StandardCharsets.UTF_8);
    }
}
