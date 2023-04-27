package com.max480.randomstuff.gae;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This servlet handles the everest.yaml validation service, and validates everest.yaml files that are sent by the user.
 * It is also used by the Mod Structure Verifier bot (that calls it like an API).
 */
@WebServlet(name = "EverestYamlValidatorService", urlPatterns = {"/celeste/everest-yaml-validator"})
@MultipartConfig
public class EverestYamlValidatorService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(EverestYamlValidatorService.class);
    private final SecureRandom random = new SecureRandom();

    // these are the fields in Everest's EverestModuleMetadata that are most commonly used in everest.yaml.
    public static class EverestModuleMetadata {
        public String Name;
        public String Version;
        public String LatestVersion;
        public String UpdatedVersion;
        public List<EverestModuleMetadata> Dependencies;
        public List<EverestModuleMetadata> OptionalDependencies;

        public Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("Name", Name);
            result.put("Version", Version);
            result.put("LatestVersion", LatestVersion);

            if (Dependencies != null) {
                result.put("Dependencies", Dependencies.stream().map(EverestModuleMetadata::toMap).collect(Collectors.toList()));
            }
            if (OptionalDependencies != null) {
                result.put("OptionalDependencies", OptionalDependencies.stream().map(EverestModuleMetadata::toMap).collect(Collectors.toList()));
            }

            return result;
        }
    }

    /**
     * This object allows to parse and print out version numbers like ones from C#.
     */
    private static class Version {
        public int[] parts;
        public final String versionString;

        public Version(String versionString) {
            this.versionString = versionString;

            // Everest ignores everything after the - if present.
            if (versionString.contains("-")) {
                versionString = versionString.substring(0, versionString.indexOf("-"));
            }

            String[] split = versionString.split("\\.");

            if (split.length < 2 || split.length > 4) {
                throw new IllegalArgumentException("Cannot parse version number \"" + versionString + "\": " +
                        "there are " + split.length + " part(s) which is not between 2 and 4.");
            }

            parts = new int[4];

            for (int i = 0; i < split.length; i++) {
                try {
                    parts[i] = Integer.parseInt(split[i]);
                    if (parts[i] < 0) {
                        throw new IllegalArgumentException("Part " + (i + 1) + " of version number \"" + versionString + "\" is negative!");
                    }
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
        // generate a sample everest.yaml that depends on latest Everest stable
        try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/latest-everest-versions.json"));
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            YamlUtil.dump(Collections.singletonList(ImmutableMap.of(
                    "Name", "YourModName",
                    "Version", "1.0.0",
                    "Dependencies", Collections.singletonList(ImmutableMap.of(
                            "Name", "Everest",
                            "Version", "1." + new JSONObject(IOUtils.toString(is, UTF_8)).getInt("stable") + ".0"
                    ))
            )), os);

            request.setAttribute("sampleYaml", os.toString(UTF_8));
        }

        // we need a nonce for downloading the sample YAML through JS
        addNonce(request, response);

        PageRenderer.render(request, response, "everest-yaml-validator", "everest.yaml validator",
                "Check if your everest.yaml is valid by sending it on this page.", true);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part filePart = null;
        try {
            filePart = request.getPart("file");
        } catch (ServletException e) {
            log.warn("Failed to get file part", e);
        }

        // output format can be either "html" or "json".
        String outputFormat = null;
        try {
            Part outputFormatPart = request.getPart("outputFormat");
            if (outputFormatPart != null) {
                outputFormat = IOUtils.toString(outputFormatPart.getInputStream(), UTF_8);
            }
        } catch (ServletException e) {
            log.warn("Failed to get output format part", e);
        }

        if (filePart != null && Arrays.asList("json", "html").contains(outputFormat)) {
            Map<String, Object> attributes = new HashMap<>();

            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

            // try parsing given everest.yaml as YAML, and catch any exception that could happen.
            List<EverestModuleMetadata> metadatas = null;
            try (InputStream is = filePart.getInputStream()) {
                List<Map<String, Object>> metadatasUnparsed = YamlUtil.loadNoFloats(is);

                if (metadatasUnparsed == null || metadatasUnparsed.isEmpty()) {
                    throw new Exception("The everest.yaml file is empty.");
                }

                metadatas = recursiveCast(metadatasUnparsed, true);
            } catch (Exception e) {
                attributes.put("parseError", e.getMessage());
            }

            if (metadatas != null) {
                // load the mod database to check if dependencies exist there.
                Map<String, Object> databaseUnparsed = YamlUtil.load(Files.newInputStream(Paths.get("/shared/celeste/updater/everest-update.yaml")));
                List<EverestModuleMetadata> database = databaseUnparsed
                        .entrySet().stream()
                        .map(entry -> {
                            EverestModuleMetadata metadata = new EverestModuleMetadata();
                            metadata.Name = entry.getKey();
                            metadata.Version = ((Map<String, String>) entry.getValue()).get("Version");
                            return metadata;
                        })
                        .collect(Collectors.toCollection(ArrayList::new));

                JSONObject everestVersions;
                try (InputStream is = Files.newInputStream(Paths.get("/shared/celeste/latest-everest-versions.json"))) {
                    everestVersions = new JSONObject(IOUtils.toString(is, UTF_8));
                }

                // add entries that don't come from the database: Celeste, Everest, and things declared by the yaml itself.
                {
                    EverestModuleMetadata metadata = new EverestModuleMetadata();
                    metadata.Name = "Celeste";
                    metadata.Version = "1.4.0.0";
                    database.add(metadata);
                }
                {
                    EverestModuleMetadata metadata = new EverestModuleMetadata();
                    metadata.Name = "Everest";
                    metadata.Version = "1." + getMaximumEverestVersion(everestVersions) + ".0";
                    database.add(metadata);
                }
                {
                    EverestModuleMetadata metadata = new EverestModuleMetadata();
                    metadata.Name = "EverestCore";
                    metadata.Version = "1." + everestVersions.getInt("core") + ".0";
                    database.add(metadata);
                }
                for (EverestModuleMetadata mod : metadatas) {
                    try {
                        new Version(mod.Version);
                        database.add(mod);
                    } catch (IllegalArgumentException e) {
                        // skip! we don't want mods with an invalid Version in our database.
                    }
                }

                List<String> problems = new ArrayList<>();

                // check everest.yaml names against names that are recognized by Everest and the mod updater.
                if (!fileName.equals("everest.yaml") && !fileName.equals("everest.yml")) {
                    problems.add("Your file is named \"" + fileName + "\", so it won't be recognized as an everest.yaml file. Rename it.");
                }

                for (EverestModuleMetadata mod : metadatas) {
                    // check for characters forbidden in file names.
                    if (mod.Name.contains("/") || mod.Name.contains("\\") || mod.Name.contains("*") || mod.Name.contains("?") || mod.Name.contains(":")
                            || mod.Name.contains("\"") || mod.Name.contains("<") || mod.Name.contains(">") || mod.Name.contains("|")) {

                        problems.add("Your mod name, \"" + mod.Name + "\", contains characters that aren't allowed in file names. That will" +
                                " cause trouble with the 1-click installer. Make sure to remove those characters: / \\ * ? : \" < > |");
                    }

                    try {
                        new Version(mod.Version);
                    } catch (IllegalArgumentException e) {
                        problems.add("The version of your mod, \"" + mod.Version + "\", does not have a valid format." +
                                " Valid formats are x.x, x.x.x or x.x.x.x, each x being a number.");
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

                                    if (dependency.Name.equals("Everest")) {
                                        // the most relevant version for Everest might be latest stable rather than latest dev.
                                        dependency.UpdatedVersion = "1." + getUpdatedEverestVersion(everestVersions, requiredVersion.parts[1]) + ".0";
                                    } else {
                                        dependency.UpdatedVersion = databaseDependency.Version;
                                    }
                                }
                            }
                        }
                    }
                }

                if (!problems.isEmpty()) {
                    attributes.put("validationErrors", problems);
                } else {
                    if ("json".equals(outputFormat)) {
                        attributes.put("modInfo", metadatas.stream().map(EverestModuleMetadata::toMap).collect(Collectors.toList()));
                    } else {
                        attributes.put("modInfo", metadatas);
                    }

                    // check if all mods are in their latest versions...
                    boolean allDependenciesAreUpToDate = true;
                    for (EverestModuleMetadata metadata : metadatas) {
                        for (EverestModuleMetadata dependency : metadata.Dependencies) {
                            if (!dependency.Version.equals(dependency.UpdatedVersion)) {
                                allDependenciesAreUpToDate = false;
                                break;
                            }
                        }
                        for (EverestModuleMetadata dependency : metadata.OptionalDependencies) {
                            if (!dependency.Version.equals(dependency.UpdatedVersion)) {
                                allDependenciesAreUpToDate = false;
                                break;
                            }
                        }
                    }

                    // ... and if not, generate a yaml file with all the latest versions in it.
                    if (!allDependenciesAreUpToDate) {
                        final List<Map<String, Object>> latestVersionsYaml = metadatas.stream()
                                .map(mod -> {
                                    Map<String, Object> updatedYaml = new LinkedHashMap<>();
                                    updatedYaml.put("Name", mod.Name);
                                    updatedYaml.put("Version", mod.Version);

                                    if (!mod.Dependencies.isEmpty()) {
                                        updatedYaml.put("Dependencies", mod.Dependencies.stream()
                                                .map(dependency -> ImmutableMap.of(
                                                        "Name", dependency.Name,
                                                        "Version", dependency.UpdatedVersion
                                                ))
                                                .collect(Collectors.toList()));
                                    }

                                    if (!mod.OptionalDependencies.isEmpty()) {
                                        updatedYaml.put("OptionalDependencies", mod.OptionalDependencies.stream()
                                                .map(dependency -> ImmutableMap.of(
                                                        "Name", dependency.Name,
                                                        "Version", dependency.UpdatedVersion
                                                ))
                                                .collect(Collectors.toList()));
                                    }

                                    return updatedYaml;
                                })
                                .collect(Collectors.toList());

                        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                            YamlUtil.dump(latestVersionsYaml, os);
                            attributes.put("latestVersionsYaml", os.toString(UTF_8));
                        }

                        if ("html".equals(outputFormat)) {
                            // in order to allow the inline script without ruining the CSP, we need to generate a nonce.
                            addNonce(request, response);
                        }
                    }
                }
            }

            if ("json".equals(outputFormat)) {
                response.setContentType("application/json");
                response.getWriter().write(new JSONObject(attributes).toString());
                return;
            } else {
                for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                    request.setAttribute(attribute.getKey(), attribute.getValue());
                }
            }
        }

        PageRenderer.render(request, response, "everest-yaml-validator", "everest.yaml validator",
                "Check if your everest.yaml is valid by sending it on this page.", request.getAttribute("latestVersionsYaml") != null);
    }

    private void addNonce(HttpServletRequest request, HttpServletResponse response) {
        byte[] nonceBytes = new byte[128];
        random.nextBytes(nonceBytes);
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        request.setAttribute("nonce", nonce);

        // then adjust the CSP to allow both accessing download.js and running the inline script powering the download button.
        response.setHeader("Content-Security-Policy", "default-src 'self'; " +
                "script-src 'self' 'nonce-" + nonce + "' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; " +
                "frame-ancestors 'none'; " +
                "object-src 'none';");
    }

    /**
     * Converts a raw List of Maps to a list of EverestModuleMetadata objects recursively
     * (the Dependencies and OptionalDependencies will be converted as well).
     */
    private static List<EverestModuleMetadata> recursiveCast(List<Map<String, Object>> list, boolean parseDependencies) {
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
            if (object.containsKey("Dependencies") && parseDependencies) {
                try {
                    metadata.Dependencies = recursiveCast((List<Map<String, Object>>) object.get("Dependencies"), false);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot parse Dependencies for " + metadata.Name + ": " + e.getMessage());
                }
            }

            // parse OptionalDependencies recursively
            if (object.containsKey("OptionalDependencies") && parseDependencies) {
                try {
                    metadata.OptionalDependencies = recursiveCast((List<Map<String, Object>>) object.get("OptionalDependencies"), false);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot parse OptionalDependencies for " + metadata.Name + ": " + e.getMessage());
                }
            }

            castedList.add(metadata);
        }

        return castedList;
    }

    /**
     * Returns the latest Everest version across all branches.
     */
    private static int getMaximumEverestVersion(JSONObject everestVersions) {
        return Math.max(
                Math.max(
                        everestVersions.getInt("dev"),
                        everestVersions.getInt("beta")
                ),
                Math.max(
                        everestVersions.getInt("stable"),
                        everestVersions.getInt("core")
                )
        );
    }

    /**
     * Updates the given Everest version, using the most relevant branch:
     * - stable if the given version is older than (or same as) stable
     * - otherwise beta if the given version is older than (or same as) beta
     * - otherwise dev
     */
    private static int getUpdatedEverestVersion(JSONObject everestVersions, int currentVersion) {
        if (currentVersion <= everestVersions.getInt("stable")) {
            return everestVersions.getInt("stable");
        } else if (currentVersion <= everestVersions.getInt("beta")) {
            return everestVersions.getInt("beta");
        } else if (currentVersion <= everestVersions.getInt("dev")) {
            return everestVersions.getInt("dev");
        } else {
            return everestVersions.getInt("core");
        }
    }
}
