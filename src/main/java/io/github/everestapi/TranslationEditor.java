package io.github.everestapi;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class TranslationEditor {
    public abstract LinkedHashMap<String, String> readLanguageEntries(Path root, String lang) throws IOException;

    protected abstract void editLanguageEntry(Path root, String lang, String key, String oldValue, String newValue) throws IOException;

    protected void open(Path root, String langRef, String langEdit) throws IOException {
        Map<String, String> entriesRef = readLanguageEntries(root, langRef);
        Map<String, String> entriesEdit = readLanguageEntries(root, langEdit);

        Set<String> allKeys = new LinkedHashSet<>(entriesRef.keySet());
        allKeys.addAll(entriesEdit.keySet());

        JFrame frame = new JFrame("Language Editor - " + langRef + " to " + langEdit);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel contents = new JPanel();
        contents.setLayout(new GridLayout(allKeys.size(), 3, 10, 0));
        for (String key : allKeys) {
            JLabel ref = new JLabel("<html>" + entriesRef.get(key) + "</html>");
            JLabel edit = new JLabel("<html>" + entriesEdit.get(key) + "</html>");
            JButton button = new JButton("Edit");
            ref.setPreferredSize(new Dimension(500, 100));
            edit.setPreferredSize(new Dimension(500, 100));

            button.addActionListener(e -> editorPopup(key, langRef, langEdit, ref, edit, entriesRef, entriesEdit, root));
            contents.add(ref);
            contents.add(edit);
            contents.add(button);
        }
        frame.setContentPane(new JScrollPane(contents));
        frame.setMinimumSize(new Dimension(600, 400));
        frame.pack();
        frame.setVisible(true);
    }

    private void editorPopup(String key, String langRef, String langEdit, JLabel ref, JLabel edit, Map<String, String> entriesRef, Map<String, String> entriesEdit, Path root) {
        JFrame frame = new JFrame("Edit - " + key);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.setLayout(new BorderLayout());

        JTextArea editArea = new JTextArea(entriesEdit.get(key));
        frame.add(new JScrollPane(editArea), BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        frame.add(ok, BorderLayout.SOUTH);
        ok.addActionListener(e -> {
            try {
                editLanguageEntry(root, langEdit, key, entriesEdit.get(key), editArea.getText());

                entriesEdit.put(key, editArea.getText());
                edit.setText("<html>" + editArea.getText() + "</html>");
                if (langRef.equals(langEdit)) {
                    ref.setText("<html>" + editArea.getText() + "</html>");
                    entriesRef.put(key, editArea.getText());
                }
                frame.dispose();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error while saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        frame.pack();
        frame.setMinimumSize(new Dimension(300, 200));
        frame.setVisible(true);
    }

    protected void searchAndReplace(Path file, String search, String replace) throws IOException {
        String langFileContents = Files.readString(file);
        langFileContents = langFileContents.replace(search, replace);
        Files.writeString(file, langFileContents);
    }
}
