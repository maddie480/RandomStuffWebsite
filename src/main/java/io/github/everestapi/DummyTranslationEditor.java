package io.github.everestapi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public class DummyTranslationEditor {
    public static void main(String[] args) throws IOException {
        new TranslationEditor() {
            @Override
            protected LinkedHashMap<String, String> readLanguageEntries(Path root, String lang) {
                LinkedHashMap<String, String> result = new LinkedHashMap<>();
                result.put("key1", "value1 for " + lang);
                result.put("key2", lang + " has value2 too!\n\nWow");
                return result;
            }

            @Override
            protected void editLanguageEntry(Path root, String lang, String key, String oldValue, String newValue) {
                System.out.println("I swear I edited " + key + " in " + lang + " from " + oldValue + " to " + newValue);
            }
        }.open(null, "en", "fr");
    }
}
