package ovh.maddie480.randomstuff.frontend.quest;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mod {
    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String modUrl;
    private final String imageUrl;
    private final String webPage;
    private final String extractDir;
    private final boolean needTexmod;
    private final boolean hasCheckpointSupport;
    private final boolean hasSpeedmod;
    private final boolean isCustom;

    Mod(String csvLine, boolean fromCustomFile) {
        String[] line = csvLine.split(";");
        id = csvToString(line[0]);
        name = csvToString(line[1]);
        version = csvToString(line[2]);
        author = csvToString(line[3]);
        description = csvToString(line[4]);
        modUrl = csvToString(line[5]);
        imageUrl = csvToString(line[6]);
        webPage = csvToString(line[7]);
        extractDir = csvToString(line[8]);
        needTexmod = Boolean.parseBoolean(line[9]);
        hasCheckpointSupport = Boolean.parseBoolean(line[10]);
        hasSpeedmod = Boolean.parseBoolean(line[11]);
        isCustom = fromCustomFile;
    }

    private String csvToString(String csv) {
        return csv.replace("\\n", "<br>").replace("<pv>", ";");
    }

    private String stringToCsv(String string) {
        return string.replace("<br>", "\\n").replace(";", "<pv>");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getModUrl() {
        return modUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getWebPage() {
        return webPage;
    }

    public String getExtractDir() {
        return extractDir;
    }

    public boolean isNeedTexmod() {
        return needTexmod;
    }

    public boolean isHasCheckpointSupport() {
        return hasCheckpointSupport;
    }

    public boolean isHasSpeedmod() {
        return hasSpeedmod;
    }

    public boolean isCustom() {
        return isCustom;
    }

    @Override
    public String toString() {
        return Stream.of(id, name, version, author, description, modUrl, imageUrl, webPage, extractDir,
                        needTexmod, hasCheckpointSupport, hasSpeedmod)
                .map(item -> stringToCsv(item.toString()))
                .collect(Collectors.joining(";"));
    }
}
