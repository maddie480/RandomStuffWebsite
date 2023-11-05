package ovh.maddie480.randomstuff.frontend;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Integrates the Lua Cutscenes documentation to the web app at build time.
 */
public class LuaCutscenesDocumentationIntegrator {
    private static final Logger logger = Logger.getLogger("LuaCutscenesDocumentationIntegrator");

    public static void main(String[] args) throws IOException {
        String luaCutscenesDownloadUrl;
        try (InputStream is = ConnectionUtils.openStreamWithTimeout("https://maddie480.ovh/celeste/everest_update.yaml")) {
            Map<String, Map<String, Object>> db = YamlUtil.load(is);
            luaCutscenesDownloadUrl = db.get("LuaCutscenes").get("URL").toString();
        }

        // download Lua Cutscenes and go through its files
        logger.info("Downloading Lua Cutscenes from " + luaCutscenesDownloadUrl + "...");
        try (ZipInputStream zip = new ZipInputStream(PrepareForRadioLNJ.getFullInputStreamWithRetry(luaCutscenesDownloadUrl))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).startsWith("documentation/")) {
                    // that's part of the docs! we want to include that.
                    Path filePath = Paths.get("resources/lua-cutscenes-documentation").resolve(entry.getName().substring(14));
                    logger.info("Integrating " + filePath);
                    Files.createDirectories(filePath.getParent());

                    try (OutputStream os = Files.newOutputStream(filePath)) {
                        IOUtils.copy(zip, os);
                    }
                }
            }
        }
    }
}
