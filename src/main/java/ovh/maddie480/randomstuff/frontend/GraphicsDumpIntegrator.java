package ovh.maddie480.randomstuff.frontend;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Integrates the graphics dump from Google Drive to the web app at build time.
 */
public class GraphicsDumpIntegrator {
    private static final Logger logger = Logger.getLogger("GraphicsDumpIntegrator");

    public static void main(String[] args) throws IOException {
        List<String> filesList = new LinkedList<>();

        try (InputStream is = ConnectionUtils.openStreamWithTimeout("https://www.googleapis.com/drive/v3/files/1ITwCI2uJ7YflAG0OwBR4uOUEJBjwTCet?key=" + System.getenv("GOOGLE_DRIVE_API_KEY") + "&alt=media");
             ZipInputStream zip = new ZipInputStream(is)) {

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    Path target = Paths.get("resources/vanilla-graphics-dump").resolve(entry.getName());
                    Files.createDirectories(target.getParent());

                    try (OutputStream os = Files.newOutputStream(target)) {
                        logger.info("Extracting file " + entry.getName() + "...");
                        IOUtils.copy(zip, os);
                    }

                    filesList.add(entry.getName());
                }
            }
        }

        logger.info("Writing files list with " + filesList.size() + " entries...");
        try (OutputStream os = Files.newOutputStream(Paths.get("resources/vanilla-graphics-dump/list.json"))) {
            IOUtils.write(new JSONArray(filesList).toString(), os, StandardCharsets.UTF_8);
        }
    }
}
