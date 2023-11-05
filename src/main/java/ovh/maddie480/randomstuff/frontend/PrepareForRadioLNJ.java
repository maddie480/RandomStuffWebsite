package ovh.maddie480.randomstuff.frontend;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PrepareForRadioLNJ {
    private static final Logger logger = Logger.getLogger("PrepareForRadioLNJ");

    public static void main(String[] args) throws IOException, InterruptedException {
        if (System.getenv("RADIO_LNJ_SOURCES") == null) return;

        Path targetDirectory = Paths.get("resources/music");
        Files.createDirectory(targetDirectory);

        int musicIndex = 0;

        JSONArray metadata = new JSONArray();

        for (Object item : new JSONArray(System.getenv("RADIO_LNJ_SOURCES"))) {
            JSONObject source = (JSONObject) item;

            if (source.getBoolean("useYoutubeDl")) {
                logger.info("Downloading from YouTube playlist at " + source.getString("url"));

                Path ytDlTarget = Paths.get("/tmp/yt-dlp-tmp");
                Files.createDirectory(ytDlTarget);

                new ProcessBuilder("/tmp/yt-dlp", "-f", "bestaudio*", "-x", "--audio-format", "mp3", "--no-windows-filenames", source.getString("url"))
                        .inheritIO()
                        .directory(ytDlTarget.toFile())
                        .start()
                        .waitFor();

                try (Stream<Path> downloadedFiles = Files.list(ytDlTarget)) {
                    for (Path file : downloadedFiles.toList()) {
                        Path targetFile = targetDirectory.resolve(musicIndex + ".mp3");
                        musicIndex++;

                        Files.copy(file, targetFile);

                        JSONObject meta = new JSONObject();
                        meta.put("trackName", source.getString("prefix") +
                                file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf("[") - 1));
                        meta.put("path", "/music/" + targetFile.getFileName().toString());
                        meta.put("duration", getMusicDuration(targetFile));

                        logger.info("Track info: " + meta.toString(2));
                        metadata.put(meta);
                    }
                }

                FileUtils.deleteDirectory(ytDlTarget.toFile());
            } else {
                logger.info("Downloading zip from " + source.getString("url"));

                try (ZipInputStream zip = new ZipInputStream(getFullInputStreamWithRetry(source.getString("url")))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        Path temp = Paths.get("/tmp/stuff.ogg");

                        try (OutputStream os = Files.newOutputStream(temp)) {
                            IOUtils.copy(zip, os);
                        }

                        Path targetFile = targetDirectory.resolve(musicIndex + ".mp3");
                        musicIndex++;

                        new ProcessBuilder("ffmpeg", "-i", "/tmp/stuff.ogg", targetFile.toAbsolutePath().toString())
                                .inheritIO()
                                .start()
                                .waitFor();

                        Files.delete(temp);

                        JSONObject meta = new JSONObject();
                        meta.put("trackName", source.getString("prefix") + entry.getName().substring(0, entry.getName().length() - 4));
                        meta.put("path", "/music/" + targetFile.getFileName().toString());
                        meta.put("duration", getMusicDuration(targetFile));

                        logger.info("Track info: " + meta.toString(2));
                        metadata.put(meta);
                    }
                }
            }
        }

        try (OutputStream os = Files.newOutputStream(Paths.get("radio_lnj_meta.json"))) {
            IOUtils.write(metadata.toString(), os, StandardCharsets.UTF_8);
        }
    }

    private static int getMusicDuration(Path musicPath) throws IOException {
        Process p = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1",
                musicPath.toAbsolutePath().toString()).start();

        double result;
        try (InputStream is = p.getInputStream()) {
            result = Double.parseDouble(IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        return (int) (result * 1000.);
    }

    /**
     * Downloads the entire file which link has been given into memory, with up to 3 tries,
     * then returns an input stream to read the downloaded bytes from.
     */
    public static InputStream getFullInputStreamWithRetry(String url) throws IOException {
        for (int i = 1; i < 3; i++) {
            try (InputStream is = ConnectionUtils.openStreamWithTimeout(url)) {
                return new ByteArrayInputStream(IOUtils.toByteArray(is));
            } catch (IOException e) {
                logger.warning("I/O exception while getting file contents (try " + i + "/3). " + e);

                // wait a bit before retrying
                try {
                    logger.info("Waiting " + (i * 5) + " seconds before next try.");
                    Thread.sleep(i * 5000);
                } catch (InterruptedException e2) {
                    logger.warning("Sleep interrupted: " + e2);
                }
            }
        }

        // 3rd try: this time, if it crashes, let it crash
        try (InputStream is = ConnectionUtils.openStreamWithTimeout(url)) {
            return new ByteArrayInputStream(IOUtils.toByteArray(is));
        }
    }
}
