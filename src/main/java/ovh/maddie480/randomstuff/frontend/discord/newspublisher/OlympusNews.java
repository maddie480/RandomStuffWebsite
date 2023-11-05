package ovh.maddie480.randomstuff.frontend.discord.newspublisher;

import org.apache.commons.io.IOUtils;
import ovh.maddie480.randomstuff.frontend.YamlUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record OlympusNews(String slug, String title, String image, String link, String shortDescription,
                          String longDescription) {

    public static OlympusNews readFrom(String filename, InputStream is) throws IOException {
        String data = IOUtils.toString(is, StandardCharsets.UTF_8);

        // split between data, preview and full text
        String[] split = data.split("\n---\n", 3);
        data = split[0];
        String preview = split[1].trim();
        String text = split.length < 3 ? null : split[2].trim();

        // parse the data part
        Map<String, String> dataParsed;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            dataParsed = YamlUtil.load(bis);
        }

        return new OlympusNews(
                filename.substring(0, filename.length() - 3),
                dataParsed.get("title"),
                dataParsed.get("image"),
                dataParsed.get("link"),
                preview.isEmpty() ? null : preview,
                text == null || text.isEmpty() ? null : text
        );
    }

    public void writeTo(OutputStream os) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            bw.write("---\n");

            Map<String, Object> data = new HashMap<>();
            if (title != null) data.put("title", title);
            if (image != null) data.put("image", image);
            if (link != null) data.put("link", link);
            data.put("ignore", false);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                YamlUtil.dump(data, bos);
                bw.write(bos.toString(StandardCharsets.UTF_8));
            }

            bw.write("\n---\n\n");
            if (shortDescription != null) bw.write(shortDescription);

            if (longDescription != null) {
                bw.write("\n\n---\n\n");
                bw.write(longDescription);
            }
        }
    }
}
