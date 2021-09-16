package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This servlet handles the everest.yaml validation service, and validates everest.yaml files that are sent by the user.
 * It is also used by the Mod Structure Verifier bot (that calls it like an API).
 */
@WebServlet(name = "EverestYamlValidatorService", urlPatterns = {"/celeste/everest-yaml-validator"})
@MultipartConfig
public class EverestYamlValidatorService extends HttpServlet {
    private final Logger logger = Logger.getLogger("EverestYamlValidatorService");

    // these are the fields in Everest's EverestModuleMetadata that are most commonly used in everest.yaml.
    public static class EverestModuleMetadata {
        public String Name;
        public String Version;
        public String LatestVersion;
        public List<EverestModuleMetadata> Dependencies;
        public List<EverestModuleMetadata> OptionalDependencies;
    }

    /**
     * This object allows to parse and print out version numbers like ones from C#.
     */
    private static class Version {
        public int[] parts;
        public String versionString;

        public Version(String versionString) {
            this.versionString = versionString;

            String[] split = versionString.split("\\.");

            if (split.length < 2 || split.length > 4) {
                throw new IllegalArgumentException("Cannot parse version number \"" + versionString + "\": " +
                        "there are " + split.length + " part(s) which is not between 2 and 4.");
            }

            parts = new int[4];

            for (int i = 0; i < split.length; i++) {
                try {
                    parts[i] = Integer.parseInt(split[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Part " + (i + 1) + " of version number \"" + versionString + "\" isn't an integer.");
                }
            }
        }

        /**
         * Checks if this version satisfies the dependency on the given version (major version matches, minor version is higher).
         * For example, 1.3.0 satisfies the dependency on 1.2.0 but not on 1.3.1 or 0.5.0.
         */
        public String satisfiesDependencyOn(Version other) {
            if (parts[0] != other.parts[0]) {
                return "Latest version in database (" + versionString + ") has different major version as version " + other.versionString +
                        " defined in your everest.yaml.";
            } else {
                for (int i = 1; i < 4; i++) {
                    if (parts[i] < other.parts[i]) {
                        return "Latest version in database (" + versionString + ") is lower than version " + other.versionString +
                                " defined in your everest.yaml.";
                    } else if (parts[i] > other.parts[i]) {
                        break;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher("/WEB-INF/everest-yaml-validator.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part filePart = null;
        try {
            filePart = request.getPart("file");
        } catch (ServletException e) {
            logger.warning("Failed to get file part: " + e.toString());
        }

        if (filePart != null) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String fileContent = IOUtils.toString(filePart.getInputStream(), UTF_8);

            // try parsing given everest.yaml as YAML, and catch any exception that could happen.
            List<EverestModuleMetadata> metadatas = null;
            try {
                List<Map<String, Object>> metadatasUnparsed = new Yaml().load(fileContent);

                if (metadatasUnparsed == null || metadatasUnparsed.isEmpty()) {
                    throw new Exception("The everest.yaml file is empty.");
                }

                metadatas = recursiveCast(metadatasUnparsed);
            } catch (Exception e) {
                request.setAttribute("parseError", e.getMessage());
            }

            if (metadatas != null) {
                // load the mod database to check if dependencies exist there.
                Map<String, Object> databaseUnparsed = new Yaml().load(new java.net.URL("https://max480-random-stuff.appspot.com/celeste/everest_update.yaml").openStream());
                List<EverestModuleMetadata> database = databaseUnparsed
                        .entrySet().stream()
                        .map(entry -> {
                            EverestModuleMetadata metadata = new EverestModuleMetadata();
                            metadata.Name = entry.getKey();
                            metadata.Version = ((Map<String, String>) entry.getValue()).get("Version");
                            return metadata;
                        })
                        .collect(Collectors.toList());

                List<String> problems = new ArrayList<>();

                // check everest.yaml names against names that are recognized by Everest and the mod updater.
                if (fileName != null && !fileName.equals("everest.yaml") && !fileName.equals("everest.yml") && !fileName.equals("multimetadata.yaml")) {
                    problems.add("Your file is named \"" + fileName + "\", so it won't be recognized as an everest.yaml file. Rename it.");
                }

                for (EverestModuleMetadata mod : metadatas) {
                    // check for characters forbidden in file names.
                    if (mod.Name.contains("/") || mod.Name.contains("\\") || mod.Name.contains("*") || mod.Name.contains("?") || mod.Name.contains(":")
                            || mod.Name.contains("\"") || mod.Name.contains("<") || mod.Name.contains(">") || mod.Name.contains("|")) {

                        problems.add("Your mod name, \"" + mod.Name + "\", contains characters that aren't allowed in file names. That will" +
                                " cause trouble with the 1-click installer. Make sure to remove those characters: / \\ * ? : \" < > |");
                    }

                    // I don't want NullPointerExceptions
                    if (mod.Dependencies == null) {
                        mod.Dependencies = new ArrayList<>();
                    }
                    if (mod.OptionalDependencies == null) {
                        mod.OptionalDependencies = new ArrayList<>();
                    }

                    // let's check every dependency, optional or not!
                    ArrayList<EverestModuleMetadata> join = new ArrayList<>(mod.Dependencies);
                    join.addAll(mod.OptionalDependencies);
                    for (EverestModuleMetadata dependency : join) {
                        // look for the dependency in the mod database.
                        EverestModuleMetadata databaseDependency = database.stream()
                                .filter(entry -> entry.Name.equals(dependency.Name)).findFirst().orElse(null);

                        // the Everest dependency has to be checked against Azure, not the mod database.
                        if (dependency.Name.equals("Everest")) {
                            databaseDependency = new EverestModuleMetadata();
                            databaseDependency.Version = "1." + getEverestVersion() + ".0";
                        }
                        // and Celeste exists, obviously
                        if (dependency.Name.equals("Celeste")) {
                            databaseDependency = new EverestModuleMetadata();
                            databaseDependency.Version = "1.4.0.0";
                        }

                        // unreleased mods that have to be checked independently
                        if (dependency.Name.equals("StrawberryJam2021")) {
                            databaseDependency = new EverestModuleMetadata();
                            databaseDependency.Version = "1.0.0";
                        }
                        if (dependency.Name.equals("GravityHelper")) {
                            databaseDependency = new EverestModuleMetadata();
                            try (InputStream is = authenticatedGitHubRequest("https://api.github.com/repos/swoolcock/GravityHelper/tags")) {
                                JSONArray tags = new JSONArray(IOUtils.toString(is, UTF_8));
                                databaseDependency.Version = tags.getJSONObject(0).getString("name");
                            }
                        }

                        if (databaseDependency == null) {
                            problems.add("One of your dependencies, \"" + dependency.Name + "\", does not exist in the database." +
                                    " Ensure you use the name this dependency uses in its own everest.yaml.");
                        } else {
                            Version requiredVersion = null, databaseVersion = null;
                            // try parsing the version in the everest.yaml
                            try {
                                requiredVersion = new Version(dependency.Version);
                            } catch (IllegalArgumentException e) {
                                problems.add("The version of \"" + dependency.Name + "\" you're requiring, \"" + dependency.Version + "\", does not have a valid format." +
                                        " You can replace it with \"" + databaseDependency.Version + "\", which is the latest version of it.");
                            }
                            // try parsing the version in the mod database (this one doesn't check for valid version numbers aaa)
                            try {
                                databaseVersion = new Version(databaseDependency.Version);
                            } catch (IllegalArgumentException e) {
                                problems.add("The version of \"" + databaseDependency.Name + "\" in the database, \"" + databaseDependency.Version + "\", does not have a valid format." +
                                        " Please report it to the mod author.");
                            }

                            // check that the version in mod database can satisfy the dependency
                            if (requiredVersion != null && databaseVersion != null) {
                                String problem = databaseVersion.satisfiesDependencyOn(requiredVersion);
                                if (problem != null) {
                                    problems.add("The dependency downloader won't be able to get the version you requested for \"" + dependency.Name + "\": " + problem);
                                } else {
                                    dependency.LatestVersion = databaseDependency.Version;
                                }
                            }
                        }
                    }
                }

                if (!problems.isEmpty()) {
                    request.setAttribute("validationErrors", problems);
                } else {
                    request.setAttribute("modInfo", metadatas);
                }
            }
        }

        request.getRequestDispatcher("/WEB-INF/everest-yaml-validator.jsp").forward(request, response);
    }

    private InputStream authenticatedGitHubRequest(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (Constants.GITHUB_USERNAME + ":" + Constants.GITHUB_PERSONAL_ACCESS_TOKEN).getBytes(UTF_8)));
        return connection.getInputStream();
    }

    /**
     * Converts a raw List of Maps to a list of EverestModuleMetadata objects recursively
     * (the Dependencies and OptionalDependencies will be converted as well).
     */
    private static List<EverestModuleMetadata> recursiveCast(List<Map<String, Object>> list) {
        List<EverestModuleMetadata> castedList = new ArrayList<>(list.size());
        for (Map<String, Object> object : list) {
            EverestModuleMetadata metadata = new EverestModuleMetadata();

            // parse Name
            try {
                metadata.Name = object.getOrDefault("Name", null).toString();
                if (metadata.Name == null) {
                    throw new Exception("The mod has no Name");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot parse Name: " + e.getMessage());
            }

            // parse Version
            try {
                metadata.Version = object.getOrDefault("Version", null).toString();
                if (metadata.Version == null) {
                    throw new Exception("The mod has no Version");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot parse Version for " + metadata.Name + ": " + e.getMessage());
            }

            // parse Dependencies recursively
            if (object.containsKey("Dependencies")) {
                try {
                    metadata.Dependencies = recursiveCast((List<Map<String, Object>>) object.get("Dependencies"));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot parse Dependencies for " + metadata.Name + ": " + e.getMessage());
                }
            }

            // parse OptionalDependencies recursively
            if (object.containsKey("OptionalDependencies")) {
                try {
                    metadata.OptionalDependencies = recursiveCast((List<Map<String, Object>>) object.get("OptionalDependencies"));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot parse OptionalDependencies for " + metadata.Name + ": " + e.getMessage());
                }
            }

            castedList.add(metadata);
        }

        return castedList;
    }

    /**
     * Checks Azure to get the latest Everest version.
     */
    private static int getEverestVersion() throws IOException {
        JSONObject object = new JSONObject(IOUtils.toString(new URL("https://dev.azure.com/EverestAPI/Everest/_apis/build/builds?api-version=5.0").openStream(), UTF_8));
        JSONArray versionList = object.getJSONArray("value");
        int latest = 0;
        for (Object version : versionList) {
            JSONObject versionObj = (JSONObject) version;
            String reason = versionObj.getString("reason");
            if (Arrays.asList("manual", "individualCI").contains(reason)) {
                latest = Math.max(latest, versionObj.getInt("id") + 700);
            }
        }
        return latest;
    }
}
