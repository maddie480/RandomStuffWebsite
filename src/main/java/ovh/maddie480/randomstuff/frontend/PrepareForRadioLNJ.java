package ovh.maddie480.randomstuff.frontend;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.function.IOSupplier;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
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

        Path cachedRadioLNJ = Paths.get("/tmp/old-target/random-stuff-website-1.0.0/WEB-INF/classes/resources/music");
        if (Files.isDirectory(cachedRadioLNJ)) {
            copyRecursive(cachedRadioLNJ, targetDirectory);
            copyRecursive(cachedRadioLNJ.resolve("../../radio_lnj_meta.json"), Paths.get("radio_lnj_meta.json"));
            return;
        }

        Files.createDirectory(targetDirectory);

        JSONArray metadata = new JSONArray();

        for (Object item : new JSONArray(System.getenv("RADIO_LNJ_SOURCES"))) {
            JSONObject source = (JSONObject) item;

            if (source.getBoolean("fromYouTube")) {
                logger.info("Retrieving from YouTube playlist at " + source.getString("url"));

                Path ytDlTarget = Paths.get("/tmp/youtube-pouet", source.getString("url").replaceAll("[^0-9a-zA-Z]", "_"));
                try (Stream<Path> downloadedFiles = Files.list(ytDlTarget)) {
                    for (Path file : downloadedFiles.toList()) {
                        cutAndProcess(source, metadata, file, source.getString("prefix") +
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

                        cutAndProcess(source, metadata, temp,
                                source.getString("prefix") + entry.getName().substring(0, entry.getName().length() - 4));
                    }
                }
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("radio_lnj_meta.json"))) {
            metadata.write(bw);
        }
    }

    private static void cutAndProcess(JSONObject source, JSONArray output, Path inputFile, String trackName) throws IOException, InterruptedException {
        if (!source.has("cuts") || !source.getJSONObject("cuts").has(trackName)) {
            // no cutting to be done
            addMetadataTo(output, inputFile, trackName);
            return;
        }

        Path temp = Paths.get("/tmp/cutemp.mkv");

        JSONArray timeCodes = source.getJSONObject("cuts").getJSONArray(trackName);
        DecimalFormat countFormat = new DecimalFormat(StringUtils.repeat("0", Integer.toString(timeCodes.length() - 1).length()));
        for (int i = 0; i < timeCodes.length() - 1; i++) {
            cut(inputFile, temp, timeCodes.getString(i), timeCodes.getString(i + 1));
            addMetadataTo(output, temp, trackName + " - " + countFormat.format(i + 1));
        }
        Files.delete(inputFile);
    }

    private static void cut(Path source, Path destination, String from, String to) throws IOException, InterruptedException {
        // calculate the difference between both times
        int t1 = Arrays.stream(from.split(":"))
                .mapToInt(Integer::parseInt)
                .reduce((a, b) -> a * 60 + b)
                .orElse(0);

        int t2 = Arrays.stream(to.split(":"))
                .mapToInt(Integer::parseInt)
                .reduce((a, b) -> a * 60 + b)
                .orElse(0);

        int diff = t2 - t1;
        int hours = diff / 3600;
        int minutes = (diff / 60) - (hours * 60);
        int seconds = (diff % 60);

        DecimalFormat f = new DecimalFormat("00");
        String difference = (f.format(hours) + ":" + f.format(minutes) + ":" + f.format(seconds));

        // run ffmpeg to do the cut
        int exitCode = new ProcessBuilder(
                "ffmpeg",
                "-ss", from,
                "-i", source.toAbsolutePath().toString(),
                "-to", difference,
                "-c", "copy",
                destination.toAbsolutePath().toString()
        )
                .inheritIO()
                .start()
                .waitFor();

        if (exitCode != 0) throw new IOException("ffmpeg exited with code " + exitCode);
    }

    private static void addMetadataTo(JSONArray output, Path inputFile, String trackName) throws IOException, InterruptedException {
        logger.info("Processing track " + inputFile.toAbsolutePath() + " (" + trackName + ")");

        Path targetFile = targetDirectory.resolve(musicIndex + ".mp3");

        convertAndNormalize(inputFile, targetFile, trackName);
        Files.delete(inputFile);

        JSONObject meta = new JSONObject();
        meta.put("trackName", trackName);
        meta.put("path", "/music/" + targetFile.getFileName().toString());
        meta.put("duration", getMusicDuration(targetFile));

        logger.info("Track info: " + meta.toString(2));
        output.put(meta);

        musicIndex++;
    }

    static void copyRecursive(Path source, Path target) throws InterruptedException, IOException {
        int exitCode = new ProcessBuilder(
                "cp", "-rv",
                source.toAbsolutePath().toString(),
                target.toAbsolutePath().toString()
        )
                .inheritIO()
                .start()
                .waitFor();

        if (exitCode != 0) throw new IOException("cp exited with code " + exitCode);
    }

    private static void convertAndNormalize(Path source, Path target, String trackName) throws InterruptedException, IOException {
        JSONArray extraParameters = new JSONArray();
        extraParameters.put("-metadata");
        extraParameters.put("title=" + trackName);
        extraParameters.put("-metadata");
        extraParameters.put("album=Radio LNJ");

        int exitCode = new ProcessBuilder(
                "ffmpeg-normalize",
                source.toAbsolutePath().toString(),
                "-o", target.toAbsolutePath().toString(),
                "-pr", "-c:a", "mp3", "-vn",
                "-e", extraParameters.toString()
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
     * Downloads the entire file which link has been given into memory, with up to 10 tries,
     * then returns an input stream to read the downloaded bytes from.
     */
    public static InputStream getFullInputStreamWithRetry(String url) throws IOException {
        int contentLength = runWithRetry(() -> getContentLength(url));

        return runWithRetry(() -> {
            try (InputStream is = ConnectionUtils.openStreamWithTimeout(url)) {
                byte[] contents = IOUtils.toByteArray(is);
                if (contents.length != contentLength) {
                    throw new IOException("The expected size (" + contentLength + ") does not match what we got (" + contents.length + ")");
                }
                return new ByteArrayInputStream(contents);
            }
        });
    }

    private static int getContentLength(String url) throws IOException {
        HttpURLConnection con = ConnectionUtils.openConnectionWithTimeout(url);
        con.setRequestMethod("HEAD");
        con.setInstanceFollowRedirects(true);

        int responseCode = con.getResponseCode();
        if (responseCode != 200) throw new IOException("Request failed with code " + responseCode);

        String contentLength = con.getHeaderField("Content-Length");
        if (contentLength == null) throw new IOException("No Content-Length header");
        logger.info("Content-Length of " + url + " is " + contentLength);

        try {
            return Integer.parseInt(contentLength);
        } catch (NumberFormatException e) {
            throw new IOException("Could not parse Content-Length header as number", e);
        }
    }

    /**
     * Runs a task (typically a network operation), retrying up to 10 times if it throws an IOException.
     *
     * @param task The task to run and retry
     * @param <T>  The return type for the task
     * @return What the task returned
     * @throws IOException If the task failed 10 times
     */
    private static <T> T runWithRetry(IOSupplier<T> task) throws IOException {
        for (int i = 1; i < 10; i++) {
            try {
                return task.get();
            } catch (IOException e) {
                logger.warning("I/O exception while doing networking operation (try " + i + "/10).");
                e.printStackTrace();

                // wait a bit before retrying
                try {
                    logger.warning("Waiting " + (i * 5) + " seconds before next try.");
                    Thread.sleep(i * 5000);
                } catch (InterruptedException e2) {
                    logger.warning("Sleep interrupted");
                    e2.printStackTrace();
                }
            }
        }

        // 3rd try: this time, if it crashes, let it crash
        return task.get();
    }
}
