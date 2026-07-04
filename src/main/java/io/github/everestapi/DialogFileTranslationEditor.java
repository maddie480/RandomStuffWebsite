package io.github.everestapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DialogFileTranslationEditor {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("2 parameters expected: <path to repo> <language to edit>");
            System.exit(1);
        }

        getTranslationEditor().open(Paths.get(args[0]), "English", args[1]);
    }

    public static TranslationEditor getTranslationEditor() {
        return getTranslationEditor("Dialog");
    }

    static TranslationEditor getTranslationEditor(String path) {
        Pattern dialogId = Pattern.compile("\\s*(\\w+)=(.*)");

        return new TranslationEditor() {
            @Override
            public LinkedHashMap<String, String> readLanguageEntries(Path root, String lang) throws IOException {
                LinkedHashMap<String, String> result = new LinkedHashMap<>();

                try (BufferedReader reader = Files.newBufferedReader(root.resolve(path + "/" + lang + ".txt"), StandardCharsets.UTF_8)) {
                    String key = null;
                    StringBuilder value = null;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("#")) continue;

                        Matcher dialogIdMatcher = dialogId.matcher(line);
                        if (dialogIdMatcher.matches()) {
                            if (key != null) result.put(key, value.toString());

                            key = dialogIdMatcher.group(1);
                            value = new StringBuilder(dialogIdMatcher.group(2));
                        } else if (value != null) {
                            value.append("\n").append(line);
                        }
                    }
                    if (key != null) result.put(key, value.toString());
                }

                return result;
            }

            @Override
            protected void editLanguageEntry(Path root, String lang, String key, String oldValue, String newValue) throws IOException {
                searchAndReplace(root.resolve(path + "/" + lang + ".txt"),
                        key + "=" + oldValue,
                        key + "=" + newValue);
            }
        };
    }
}
