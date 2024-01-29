package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@WebServlet(name = "AssetDriveService", loadOnStartup = 11, urlPatterns = {"/celeste/asset-drive/reload", "/celeste/asset-drive/list/decals",
        "/celeste/asset-drive/list/stylegrounds", "/celeste/asset-drive/list/fgtilesets", "/celeste/asset-drive/list/bgtilesets",
        "/celeste/asset-drive/list/hires", "/celeste/asset-drive/list/misc", "/celeste/asset-drive/folders",
        "/celeste/asset-drive/last-updated", "/celeste/asset-drive/files/*", "/celeste/asset-drive/multi-download"})
public class AssetDriveService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(AssetDriveService.class);

    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    private Map<String, JSONObject> fileAssetMap = Collections.emptyMap();
    private Map<String, String> fileLists = Collections.emptyMap();
    private String folderPathsToIds = "{}";

    private final Path fileListFile = Paths.get("/shared/celeste/asset-drive/file-list.json");
    private final Path categorizedAssetsFile = Paths.get("/shared/celeste/asset-drive/categorized-assets.json");

    @Override
    public void init() {
        try {
            buildAssetMap();
        } catch (Exception e) {
            log.warn("Warming up failed!", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getRequestURI().equals("/celeste/asset-drive/reload")) {
            if (req.getQueryString() == null || !req.getQueryString().equals("key=" + SecretConstants.RELOAD_SHARED_SECRET)) {
                // invalid secret
                log.warn("Invalid key");
                resp.setStatus(403);
            } else {
                buildAssetMap();
            }
            return;
        }

        if (req.getRequestURI().equals("/celeste/asset-drive/last-updated")) {
            resp.setContentType("text/plain");
            resp.getWriter().write(
                    Files.getLastModifiedTime(categorizedAssetsFile)
                            .toInstant().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            );
            return;
        }

        if (req.getRequestURI().equals("/celeste/asset-drive/folders")) {
            resp.setContentType("application/json");
            resp.getWriter().write(folderPathsToIds);
            return;
        }

        if (req.getRequestURI().startsWith("/celeste/asset-drive/list/")) {
            resp.setContentType("application/json");
            resp.getWriter().write(fileLists.get(req.getRequestURI().substring(26)));
            return;
        }

        if (req.getRequestURI().startsWith("/celeste/asset-drive/files/")) {
            Asset asset = getAsset(req.getRequestURI().substring(27));

            if (asset != null) {
                resp.setContentType(asset.mimeType);
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + asset.fileName + "\"");

                try (InputStream is = Files.newInputStream(asset.file)) {
                    IOUtils.copy(is, resp.getOutputStream());
                    return;
                }
            }
        }

        if (req.getRequestURI().equals("/celeste/asset-drive/multi-download")) {
            if (req.getQueryString() == null || !req.getQueryString().startsWith("files=")) {
                log.warn("Missing files parameter");
                resp.setStatus(400);
                return;
            }

            List<Asset> matchingAssets = Arrays.stream(req.getQueryString().substring(6).split(","))
                    .distinct()
                    .limit(100)
                    .map(this::getAsset)
                    .toList();

            if (matchingAssets.stream().noneMatch(Objects::isNull)) {
                resp.setContentType("application/zip");

                try (ZipOutputStream os = new ZipOutputStream(resp.getOutputStream())) {
                    os.setMethod(ZipEntry.STORED); // PNG files barely compress anyway

                    for (Asset asset : matchingAssets) {
                        // Write the header. For stored entries, you need to set the size and CRC32.
                        ZipEntry entry = new ZipEntry(asset.fileName);
                        CRC32 crc = new CRC32();
                        try (InputStream is = Files.newInputStream(asset.file)) {
                            crc.update(IOUtils.toByteArray(is));
                        }
                        entry.setCrc(crc.getValue());
                        entry.setSize(Files.size(asset.file));

                        // Write the file.
                        os.putNextEntry(entry);
                        try (InputStream is = Files.newInputStream(asset.file)) {
                            IOUtils.copy(is, os);
                        }
                        os.closeEntry();
                    }
                }

                return;
            }
        }

        log.warn("Not found");
        resp.setStatus(404);
        PageRenderer.render(req, resp, "page-not-found", "Page Not Found",
                "Oops, this link seems invalid. Please try again!");
    }

    private record Asset(Path file, String fileName, String mimeType) {
    }

    private Asset getAsset(String fileId) {
        JSONObject foundFile = fileAssetMap.getOrDefault(fileId, null);
        log.debug("Looking for asset with id {}, found: {}", fileId, foundFile);

        if (foundFile != null) {
            String name = foundFile.getString("name");
            String mimeType = foundFile.getString("mimeType");

            if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                // the backend exports docx files to txt: change the MIME type and the file extension
                mimeType = "text/plain";

                if (name.toLowerCase(Locale.ROOT).endsWith(".docx")) {
                    name = name.substring(0, name.length() - 5) + ".txt";
                }
            }

            String extension = switch (mimeType) {
                case "image/png" -> "png";
                case "font/ttf" -> "ttf";
                case "text/plain" -> "txt";
                case "text/yaml" -> "yaml";
                default -> "bin";
            };

            Path file = Paths.get("/shared/celeste/asset-drive/files/" + fileId + "." + extension);
            if (Files.exists(file)) {
                return new Asset(file, name, mimeType);
            }
        }

        return null;
    }

    private void buildAssetMap() throws IOException {
        JSONArray allFiles;
        try (InputStream is = Files.newInputStream(fileListFile)) {
            allFiles = new JSONArray(IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        Map<String, JSONObject> result = new HashMap<>();
        Map<String, String> folders = new HashMap<>();

        for (Object o : allFiles) {
            JSONObject file = (JSONObject) o;

            if (file.getString("mimeType").equals(FOLDER_MIME_TYPE)) {
                folders.put(file.getString("folder") + "/" + file.getString("name"), file.getString("id"));
            } else {
                result.put(file.getString("id"), file);
            }
        }

        log.debug("Loaded asset map with {} files and {} folders.", result.size(), folders.size());

        Map<String, String> responsePerCategory = new HashMap<>();

        JSONObject allCategories;
        try (InputStream is = Files.newInputStream(categorizedAssetsFile)) {
            allCategories = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        for (String category : Arrays.asList("misc", "decals", "stylegrounds", "bgtilesets", "fgtilesets", "hires")) {
            responsePerCategory.put(category, allCategories.getJSONArray(category).toString());
        }

        fileAssetMap = result;
        folderPathsToIds = new JSONObject(folders).toString();
        fileLists = responsePerCategory;
    }
}
