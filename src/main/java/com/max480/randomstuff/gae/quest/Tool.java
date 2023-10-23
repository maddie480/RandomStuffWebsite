package com.max480.randomstuff.gae.quest;

public class Tool {
    public String name;
    public String version;
    public String author;
    public String longDescription;
    public String downloadUrl;
    public String moreInfoUrl;
    public String imageUrl;

    private String csvToString(String csv) {
        return csv.replace("\\n", "\n").replace("<pv>", ";");
    }

    Tool(String csv) {
        String[] line = csv.split(";");
        name = csvToString(line[0]);
        version = csvToString(line[1]);
        author = csvToString(line[2]);
        longDescription = csvToString(line[3]);
        downloadUrl = csvToString(line[4]);
        moreInfoUrl = csvToString(line[5]);
        if (line.length > 6) imageUrl = csvToString(line[6]);
    }
}
