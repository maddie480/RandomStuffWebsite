package ovh.maddie480.randomstuff.frontend.moddatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import ovh.maddie480.randomstuff.frontend.moddatabase.model.FileRecord;
import ovh.maddie480.randomstuff.frontend.moddatabase.model.ModRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ModDatabase implements AutoCloseable {
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

    private static final Path lockFile = Paths.get("/shared/celeste/database_lock");
    private static final Path databaseFile = Paths.get("/shared/celeste/mod-database.yaml");

    static {
        lockFile.toFile().deleteOnExit();
    }

    public final List<ModRecord> allMods;

    public ModDatabase() throws IOException {
        acquireDatabaseLock();

        try (BufferedReader br = Files.newBufferedReader(databaseFile, StandardCharsets.UTF_8)) {
            logger.debug("Loading mod database...");
            this.allMods = yaml.load(br);
        } catch (IOException e) {
            releaseDatabaseLock();
            throw e;
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

    @Override
    public void close() throws IOException {
        releaseDatabaseLock();
    }

    private static void acquireDatabaseLock() throws IOException {
        logger.debug("Waiting for database lock to be released...");
        while (!tryCreate(lockFile)) unstoppableSleep(1000);
        logger.debug("Acquired database lock!");
    }

    private static void releaseDatabaseLock() throws IOException {
        Files.delete(lockFile);
        logger.debug("Released database lock!");
    }

    private static boolean tryCreate(Path file) throws IOException {
        try {
            Files.createFile(file);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }

    static void unstoppableSleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // this should never happen anyway
        }
    }
}
