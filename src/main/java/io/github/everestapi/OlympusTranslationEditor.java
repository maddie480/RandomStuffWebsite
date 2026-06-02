package io.github.everestapi;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
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

    public static @NonNull TranslationEditor getTranslationEditor() {
        Pattern contentExtractorLua = Pattern.compile("([0-9a-z_]+) = \\[\\[([^]]+)]]", Pattern.DOTALL);
        Pattern contentExtractorSharp = Pattern.compile("\"([^\"]+)\", \"([^\"]+)\"");

        return new TranslationEditor() {
            private final Set<String> sharpKeys = new HashSet<>();

            @Override
            public LinkedHashMap<String, String> readLanguageEntries(Path root, String lang) throws IOException {
                LinkedHashMap<String, String> result = new LinkedHashMap<>();

                { // lua language file
                    String langFileContents = Files.readString(root.resolve("src/lang.lua"));
                    langFileContents = langFileContents.substring(langFileContents.indexOf("local " + lang + " = {"));
                    langFileContents = langFileContents.substring(0, langFileContents.indexOf("\n}\n"));

                    Matcher matcher = contentExtractorLua.matcher(langFileContents);
                    while (matcher.find()) {
                        result.put(matcher.group(1), matcher.group(2));
                    }
                }
                { // c# mod updater
                    String langFileContents = Files.readString(root.resolve("sharp/CmdUpdateAllMods.cs"));
                    langFileContents = langFileContents.substring(langFileContents.indexOf("\"" + lang + "\", new Dictionary<string, string> {"));
                    langFileContents = langFileContents.substring(0, langFileContents.indexOf("            }"));

                    Matcher matcher = contentExtractorSharp.matcher(langFileContents);
                    while (matcher.find()) {
                        result.put(matcher.group(1), matcher.group(2));
                        sharpKeys.add(matcher.group(1));
                    }
                }

                return result;
            }

            @Override
            protected void editLanguageEntry(Path root, String lang, String key, String oldValue, String newValue) throws IOException {
                if (sharpKeys.contains(key)) {
                    searchAndReplace(root.resolve("sharp/CmdUpdateAllMods.cs"),
                            "\"" + key + "\", \"" + oldValue + "\"",
                            "\"" + key + "\", \"" + newValue + "\"");
                } else {
                    searchAndReplace(root.resolve("src/lang.lua"),
                            key + " = [[" + oldValue + "]]",
                            key + " = [[" + newValue + "]]");
                }
            }
        };
    }
}
