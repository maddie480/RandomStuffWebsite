package io.github.everestapi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OlympusTranslationEditor {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("2 parameters expected: <path to Olympus repo> <language to edit>");
            System.exit(1);
        }

        getTranslationEditor().open(Paths.get(args[0]), "en", args[1]);
    }

    public static TranslationEditor getTranslationEditor() {
        Pattern contentExtractor = Pattern.compile("([0-9a-z_]+) = \\[\\[([^]]+)]]", Pattern.DOTALL);

        return new TranslationEditor() {
            @Override
            public LinkedHashMap<String, String> readLanguageEntries(Path root, String lang) throws IOException {
                LinkedHashMap<String, String> result = new LinkedHashMap<>();
                String langFileContents = Files.readString(root.resolve("src/lang/" + lang + ".lua"));
                Matcher matcher = contentExtractor.matcher(langFileContents);
                while (matcher.find()) {
                    result.put(matcher.group(1), matcher.group(2));
                }
                return result;
            }

            @Override
            protected void editLanguageEntry(Path root, String lang, String key, String oldValue, String newValue) throws IOException {
                searchAndReplace(root.resolve("src/lang/" + lang + ".lua"),
                        key + " = [[" + oldValue + "]]",
                        key + " = [[" + newValue + "]]");
            }
        };
    }
}
