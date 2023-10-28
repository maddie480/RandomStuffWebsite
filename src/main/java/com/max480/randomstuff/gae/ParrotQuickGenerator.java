package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ParrotQuickGenerator {
    private static final java.util.logging.Logger logger = Logger.getLogger("v");

    public static void main(String[] args) throws IOException {
        StringBuilder parrotQuickImporterPage = new StringBuilder("""
                <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Parrot Quick Importer Online</title>

                        <link rel="shortcut icon" href="https://cultofthepartyparrot.com/still/parrots/parrot.png">

                        <script src="https://code.jquery.com/jquery-3.7.1.min.js"
                            integrity="sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=" crossorigin="anonymous"></script>

                        <link rel="stylesheet" href="css/parrot-quick-importer.css">
                    </head>

                    <body>
                        <div class="content">
                            <h1>Parrot Quick Importer Online</h1>

                            <form>
                                <label for="discord">Discord</label>
                                <input id="discord" type="radio" name="type" value="discord" checked="checked">

                                <label for="gitlab">GitLab</label>
                                <input id="gitlab" type="radio" name="type" value="gitlab">

                                <label for="mattermost">Mattermost</label>
                                <input id="mattermost" type="radio" name="type" value="mattermost">

                                <input id="recherche" type="text" placeholder="Rechercher un parrot">
                            </form>

                """);

        for (Map.Entry<String, String> parrot : getParrots().entrySet()) {
            parrotQuickImporterPage
                    .append("        <a class=\"target\" title=\"")
                    .append(StringEscapeUtils.escapeHtml4(parrot.getKey()))
                    .append("\" data-target=\"")
                    .append(StringEscapeUtils.escapeHtml4(parrot.getValue()))
                    .append("\">\n")

                    .append("            <img src=\"")
                    .append(StringEscapeUtils.escapeHtml4(parrot.getValue()))
                    .append("\" height=\"32\" width=\"32\">\n")

                    .append("        </a>\n");
        }

        parrotQuickImporterPage.append("""
                        </div>
                        <script type="text/javascript" src="js/parrot-script.js"></script>
                    </body>
                </html>
                """);

        Files.createDirectories(Paths.get("resources/static"));

        try (OutputStream os = Files.newOutputStream(Paths.get("resources/static/parrot-quick-importer-online.html"))) {
            IOUtils.write(parrotQuickImporterPage.toString(), os, StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> getParrots() throws IOException {
        final Map<String, String> extraParrots = new LinkedHashMap<>();
        extraParrots.put("OSEF Parrot", "https://maddie480.ovh/static/img/osef_parrot.gif");
        extraParrots.put("AH Parrot", "https://maddie480.ovh/static/img/ah_parrot.gif");
        extraParrots.put("Flan Parrot", "https://maddie480.ovh/static/img/flan_parrot.gif");
        extraParrots.put("Zscaler Parrot", "https://maddie480.ovh/static/img/zscaler_parrot.gif");
        extraParrots.put("GitLab Parrot", "https://maddie480.ovh/static/img/gitlab_parrot.gif");
        extraParrots.put("Ember Parrot", "https://maddie480.ovh/static/img/ember_parrot.gif");

        Map<String, String> parrots = new LinkedHashMap<>();

        // the content-encoding seems to be inconsistent, and be "br" most of the time.
        // so... just retry until we get an answer we can actually read.
        for (int i = 0; i < 100 && parrots.isEmpty(); i++) {
            for (Element elt : Jsoup.connect("https://cultofthepartyparrot.com/").get().select("article li img")) {
                parrots.put(elt.attr("alt"), "https://cultofthepartyparrot.com" + elt.attr("data-src"));
            }
        }

        if (parrots.isEmpty()) throw new IOException("Where did the parrots go?");

        parrots.putAll(extraParrots);
        logger.fine("There are " + parrots.size() + " parrots in my database :parrot_parrot:");
        return parrots;
    }
}
