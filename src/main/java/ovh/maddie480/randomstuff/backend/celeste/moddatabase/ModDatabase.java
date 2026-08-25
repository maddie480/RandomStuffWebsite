package ovh.maddie480.randomstuff.backend.celeste.moddatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.model.FileRecord;
import ovh.maddie480.randomstuff.backend.celeste.moddatabase.model.ModRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ModDatabase {
    private static final Yaml yaml;
    private static final Logger logger = LoggerFactory.getLogger(ModDatabase.class);

    static {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(1024 * 1024 * 1024);
        loaderOptions.setTagInspector(tag -> tag.matches(ModRecord.class));
        loaderOptions.setMaxAliasesForCollections(1_000_000);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        yaml = new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
    }

    private static final Path databaseFile = Paths.get("/shared/celeste/mod-database.yaml");

    public final List<ModRecord> allMods;

    public ModDatabase() throws IOException {
        try (BufferedReader br = Files.newBufferedReader(databaseFile, StandardCharsets.UTF_8)) {
            logger.debug("Loading mod database...");
            this.allMods = yaml.load(br);
        }
    }

    public record ModLatestVersion(ModRecord mod, FileRecord file) {
    }

    public static List<ModLatestVersion> listLatestVersions(List<ModRecord> database) {
        return database.stream()
                .map(m -> Arrays.stream(m.files)
                        .filter(f -> f.isLeader)
                        .map(f -> new ModLatestVersion(m, f))
                        .toList())
                .flatMap(List::stream)
                .toList();
    }
}
