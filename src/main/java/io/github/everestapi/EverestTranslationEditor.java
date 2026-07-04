package io.github.everestapi;

import java.io.IOException;
import java.nio.file.Paths;

public class EverestTranslationEditor {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("2 parameters expected: <path to Everest repo> <language to edit>");
            System.exit(1);
        }

        getTranslationEditor().open(Paths.get(args[0]), "English", args[1]);
    }

    public static TranslationEditor getTranslationEditor() {
        return DialogFileTranslationEditor.getTranslationEditor("Celeste.Mod.mm/Content/Dialog");
    }
}
