package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This follows the progress of a font generation, or of a mod structure verification, and shows the result.
 */
@WebServlet(name = "TaskTrackerService", urlPatterns = {"/celeste/task-tracker/font-generate/*", "/celeste/task-tracker/mod-structure-verify/*"})
@MultipartConfig
public class TaskTrackerService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(TaskTrackerService.class);

    private final Pattern trackerPageUrlPattern = Pattern.compile("^/celeste/task-tracker/([a-z-]+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/?$");
    private final Pattern downloadPageUrlPattern = Pattern.compile("^/celeste/task-tracker/([a-z-]+)/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/download/([0-9]+)/?$");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean regexMatch = false;

        // initialize all status flags to false by default.
        request.setAttribute("taskNotFound", false);
        request.setAttribute("taskOngoing", false);
        request.setAttribute("fileNotFound", false);

        int refreshDelay = 0;

        // 1/ tracking page: /celeste/task-tracker/[type]/[id]
        Matcher trackerPageUrlMatch = trackerPageUrlPattern.matcher(request.getRequestURI());
        if (trackerPageUrlMatch.matches()) {
            regexMatch = true;

            String type = trackerPageUrlMatch.group(1);
            String id = trackerPageUrlMatch.group(2);
            request.setAttribute("type", type);
            request.setAttribute("id", id);

            Path timestampFile = Paths.get("/shared/temp/" + type + "/" + id + "-timestamp.txt");
            if (!Files.exists(timestampFile)) {
                // if this file does not exist, it means the task was never created in the first place.
                log.warn("{} does not exist => task not found!", timestampFile);
                request.setAttribute("taskNotFound", true);
            } else {
                // get the start timestamp
                long taskCreateTimestamp;
                try (InputStream is = Files.newInputStream(timestampFile)) {
                    taskCreateTimestamp = Long.parseLong(IOUtils.toString(is, StandardCharsets.UTF_8));
                }

                Path report = Paths.get("/shared/temp/" + type + "/" + id + "-" + type + "-" + id + ".json");
                if (!Files.exists(report)) {
                    // the result does not exist, it means the task is still ongoing
                    request.setAttribute("taskOngoing", true);

                    // we will refresh in a bit.
                    refreshDelay = getWaitTime(taskCreateTimestamp);
                    log.debug("Task is not finished yet, waiting for {} seconds before checking again.", refreshDelay);
                    request.setAttribute("taskCreatedAgo", formatTimeAgo(taskCreateTimestamp));
                } else {
                    // task is done!
                    log.debug("Task is finished!");

                    // parse the result
                    JSONObject result;
                    try (InputStream is = Files.newInputStream(report)) {
                        result = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                    }
                    request.setAttribute("taskResult", result.getString("responseText"));
                    request.setAttribute("taskResultType", getResultType(result.getString("responseText")));
                    request.setAttribute("attachments", getAttachmentsFor(type, result));
                }
            }
        }

        // 2/ download handling: /celeste/task-tracker/[type]/[id]/download/[index]
        Matcher downloadPageUrlMatch = downloadPageUrlPattern.matcher(request.getRequestURI());
        if (downloadPageUrlMatch.matches()) {
            regexMatch = true;

            String type = downloadPageUrlMatch.group(1);
            String id = downloadPageUrlMatch.group(2);
            int index = Integer.parseInt(downloadPageUrlMatch.group(3));

            Path report = Paths.get("/shared/temp/" + type + "/" + id + "-" + type + "-" + id + ".json");
            if (!Files.exists(report)) {
                // the task doesn't exist (or isn't over) in the first place.
                log.warn("{} does not exist => task not found!", report);
                request.setAttribute("taskNotFound", true);
            } else {
                // parse the task result
                JSONObject result;
                try (InputStream is = Files.newInputStream(report)) {
                    result = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
                }

                JSONArray attachmentNames = result.getJSONArray("attachments");
                if (index < 0 || index >= attachmentNames.length()) {
                    // we asked for a file that was out of bounds!
                    log.warn("File {} is out of range => file not found!", index);
                    request.setAttribute("fileNotFound", true);
                } else {
                    // get the file name from the task attachment list
                    String fileName = attachmentNames.getString(index);
                    log.debug("File name: {}", fileName);

                    // send the file from storage
                    response.setHeader("Content-Type", getContentType(fileName));
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    try (InputStream is = Files.newInputStream(Paths.get("/shared/temp/" + type + "/" + fileName))) {
                        IOUtils.copy(is, response.getOutputStream());
                    }
                    return;
                }
            }
        }

        if (!regexMatch) {
            // the URL the user tried to access is invalid, so let's just answer the task was not found.
            log.warn("URI does not match regex => task not found!");
            request.setAttribute("taskNotFound", true);

            request.setAttribute("type", request.getRequestURI().startsWith("/celeste/task-tracker/font-generate") ? "font-generate" : "mod-structure-verify");
        }

        if (request.getRequestURI().startsWith("/celeste/task-tracker/font-generate")) {
            PageRenderer.render(request, response, "task-tracker", "font-generator", "Celeste Font Generator – Result",
                    "This is a direct link to a Font Generator result. It will stay valid for 1 day after the generation ends.", refreshDelay);
        } else {
            PageRenderer.render(request, response, "task-tracker", "mod-structure-verifier", "Celeste Mod Structure Verifier – Result",
                    "This is a direct link to a Mod Structure Verifier result. It will stay valid for 1 day after the verification ends.", refreshDelay);
        }
    }

    private String formatTimeAgo(long timestamp) {
        long secondsAgo = (System.currentTimeMillis() - timestamp) / 1000L;

        if (secondsAgo == 0) {
            return "just now";
        } else if (secondsAgo < 60) {
            return secondsAgo + (secondsAgo == 1 ? " second ago" : " seconds ago");
        } else if (secondsAgo < 3600) {
            return (secondsAgo / 60) + ((secondsAgo / 60) == 1 ? " minute" : " minutes")
                    + " and " + (secondsAgo % 60) + ((secondsAgo % 60 == 1) ? " second" : " seconds") + " ago";
        } else {
            // if we reach that point, we have big issues.
            log.warn("This task was launched more than an hour ago!");
            return "more than an hour ago";
        }
    }

    private int getWaitTime(long timestamp) {
        long secondsAgo = (System.currentTimeMillis() - timestamp) / 1000L;

        if (secondsAgo < 30) {
            return 5;
        } else if (secondsAgo < 60) {
            return 10;
        } else if (secondsAgo < 120) {
            return 15;
        } else if (secondsAgo < 300) {
            return 30;
        } else {
            return 60;
        }
    }

    private String getResultType(String responseText) {
        if (responseText.startsWith("✅")) {
            return "success";
        } else if (responseText.startsWith("⚠")) {
            return "warning";
        } else {
            return "danger";
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        } else if (fileName.endsWith(".yaml")) {
            return "text/yaml";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

    private List<String> getAttachmentsFor(String type, JSONObject result) {
        if (type.equals("font-generate")) {
            return switch (result.getJSONArray("attachments").length()) {
                case 1 -> Collections.singletonList("Font zip file");
                case 2 -> Arrays.asList("Font zip file", "Missing characters");
                default -> Collections.emptyList();
            };
        } else if (type.equals("mod-structure-verify")) {
            List<String> attachmentList = new ArrayList<>();
            for (Object o : result.getJSONArray("attachments")) {
                String attachmentName = (String) o;
                if (attachmentName.endsWith(".yaml")) {
                    attachmentList.add("everest.yaml with added dependencies");
                } else {
                    attachmentName = attachmentName.substring(attachmentName.lastIndexOf("-") + 1, attachmentName.indexOf("_"));
                    attachmentName = attachmentName.substring(0, 1).toUpperCase(Locale.ROOT) + attachmentName.substring(1);
                    attachmentList.add("Missing characters in " + attachmentName);
                }
            }
            return attachmentList;
        }

        return Collections.emptyList();
    }
}
