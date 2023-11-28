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

    private static final Path targetDirectory = Paths.get("resources/music");
    private static int musicIndex = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (System.getenv("RADIO_LNJ_SOURCES") == null) return;

        Files.createDirectory(targetDirectory);

        JSONArray metadata = new JSONArray();

        for (Object item : new JSONArray(System.getenv("RADIO_LNJ_SOURCES"))) {
            JSONObject source = (JSONObject) item;

            if (source.getBoolean("useYoutubeDl")) {
                logger.info("Downloading from YouTube playlist at " + source.getString("url"));

                Path ytDlTarget = Paths.get("/tmp/yt-dlp-tmp");
                Files.createDirectory(ytDlTarget);

                int exitCode = new ProcessBuilder("/tmp/yt-dlp", "-f", "bestaudio*", source.getString("url"))
                        .inheritIO()
                        .directory(ytDlTarget.toFile())
                        .start()
                        .waitFor();

                if (exitCode != 0) throw new IOException("yt-dlp exited with code " + exitCode);

                try (Stream<Path> downloadedFiles = Files.list(ytDlTarget)) {
                    for (Path file : downloadedFiles.toList()) {
                        addMetadataTo(metadata, file, source.getString("prefix") +
                                file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf("[") - 1)
                                        .replace("＂", "\"")
                                        .replace("：", ":")
                                        .replace("⧸", "/")
                                        .replace("｜", "|"));
                    }
                }

                FileUtils.deleteDirectory(ytDlTarget.toFile());
            } else {
                logger.info("Downloading zip from " + source.getString("url"));

                try (ZipInputStream zip = new ZipInputStream(getFullInputStreamWithRetry(source.getString("url")))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        Path temp = Paths.get("/tmp/stuff.bin");
                        try (OutputStream os = Files.newOutputStream(temp)) {
                            IOUtils.copy(zip, os);
                        }

                        addMetadataTo(metadata, temp,
                                source.getString("prefix") + entry.getName().substring(0, entry.getName().length() - 4));
                    }
                }
            }
        }

        try (OutputStream os = Files.newOutputStream(Paths.get("radio_lnj_meta.json"))) {
            IOUtils.write(metadata.toString(), os, StandardCharsets.UTF_8);
        }
    }

    private static void addMetadataTo(JSONArray output, Path inputFile, String trackName) throws IOException, InterruptedException {
        logger.info("Processing track " + inputFile.toAbsolutePath() + " (" + trackName + ")");

        Path targetFile = targetDirectory.resolve(musicIndex + ".mp3");

        convertAndNormalize(inputFile, targetFile);
        Files.delete(inputFile);

        JSONObject meta = new JSONObject();
        meta.put("trackName", trackName);
        meta.put("path", "/music/" + targetFile.getFileName().toString());
        meta.put("duration", getMusicDuration(targetFile));

        logger.info("Track info: " + meta.toString(2));
        output.put(meta);

        musicIndex++;
    }

    private static void convertAndNormalize(Path source, Path target) throws InterruptedException, IOException {
        int exitCode = new ProcessBuilder(
                "ffmpeg-normalize",
                source.toAbsolutePath().toString(),
                "-o", target.toAbsolutePath().toString(),
                "-pr", "-c:a", "mp3", "-vn"
        )
                .inheritIO()
                .start()
                .waitFor();

        if (exitCode != 0) throw new IOException("ffmpeg-normalize exited with code " + exitCode);
    }

    private static int getMusicDuration(Path musicPath) throws InterruptedException, IOException {
        Process p = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1",
                musicPath.toAbsolutePath().toString()).start();

        double result;
        try (InputStream is = p.getInputStream()) {
            result = Double.parseDouble(IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) throw new IOException("ffprobe exited with code " + exitCode);

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
