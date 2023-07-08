package com.max480.randomstuff.gae.discord.newspublisher;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.max480.randomstuff.gae.SecretConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GitOperator {
    private static final Logger log = LoggerFactory.getLogger(GitOperator.class);

    private static final Path gitDirectory = Paths.get("/tmp/olympus_news_repo");
    private static Git gitRepository;

    public static void sshInit() {
        log.info("Configuring SSH...");

        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            @Override
            protected void configureJSch(JSch jsch) {
                try {
                    jsch.addIdentity(
                            "id_rsa",
                            SecretConstants.GITHUB_SSH_PRIVATE_KEY.getBytes(StandardCharsets.UTF_8),
                            SecretConstants.GITHUB_SSH_PUBLIC_KEY.getBytes(StandardCharsets.UTF_8),
                            null
                    );

                    ByteArrayInputStream is = new ByteArrayInputStream(SecretConstants.GITHUB_SSH_KNOWN_HOSTS.getBytes(StandardCharsets.UTF_8));
                    jsch.setKnownHosts(is);
                } catch (JSchException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void init() throws IOException {
        // == clone repository
        if (Files.isDirectory(gitDirectory)) {
            FileUtils.deleteDirectory(gitDirectory.toFile());
        }

        try {
            log.info("Cloning git repository...");
            gitRepository = Git.cloneRepository()
                    .setDirectory(gitDirectory.toFile())
                    .setBranch("main")
                    .setDepth(1)
                    .setURI("git@github.com:EverestAPI/EverestAPI.github.io.git")
                    .call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    public static List<OlympusNews> listOlympusNews() throws IOException {
        try (Stream<Path> files = Files.list(gitDirectory.resolve("olympusnews"))) {
            List<OlympusNews> result = new ArrayList<>();

            for (Path olympusNews : files
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".md"))
                    .toList()) {

                try (InputStream is = Files.newInputStream(olympusNews)) {
                    result.add(OlympusNews.readFrom(olympusNews.getFileName().toString(), is));
                }
            }

            result.sort(Comparator.comparing(OlympusNews::slug));

            return result;
        }
    }

    public static void createOlympusNews(OlympusNews news, byte[] image) throws IOException {
        log.info("Creating Olympus news...");

        // == find a unique slug

        int index = 0;

        Set<String> prefixes = listOlympusNews().stream()
                .map(n -> n.slug().substring(0, 14))
                .collect(Collectors.toSet());

        while (prefixes.contains(generatePrefix(index))) {
            index++;
        }

        String tokenizedTitle = news.title() != null ? news.title() : "untitled";
        tokenizedTitle = tokenizedTitle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
        while (tokenizedTitle.contains("--")) {
            tokenizedTitle = tokenizedTitle.replace("--", "-");
        }

        String slug = generatePrefix(index) + tokenizedTitle;
        news = new OlympusNews(slug, news.title(), news.image(), news.link(), news.shortDescription(), news.longDescription());

        // == save the image

        if (image != null) {
            String format;

            if (image.length > 8
                    && image[0] == -119 /* 0x89 */ && image[1] == 0x50 && image[2] == 0x4E && image[3] == 0x47
                    && image[4] == 0x0D && image[5] == 0x0A && image[6] == 0x1A && image[7] == 0x0A) {

                format = "png";
            } else if (image.length > 3
                    && image[0] == -1 /* 0xff */ && image[1] == -40 /* 0xd8 */ && image[2] == -1 /* 0xff */) {

                format = "jpg";
            } else {
                throw new IOException("Invalid image file!");
            }

            log.info("Image format: {}", format);

            String imagePath = "./images/" + slug + "." + format;

            try (OutputStream os = Files.newOutputStream(gitDirectory.resolve("olympusnews").resolve(imagePath))) {
                IOUtils.write(image, os);
            }

            news = new OlympusNews(news.slug(), news.title(), imagePath, news.link(), news.shortDescription(), news.longDescription());
        }

        // == save the news
        updateOlympusNews(news);
    }

    private static String generatePrefix(int index) {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "-" + new DecimalFormat("00").format(index) + "-";
    }

    public static void updateOlympusNews(OlympusNews news) throws IOException {
        log.info("Updating Olympus news: {}", news);

        try (OutputStream os = Files.newOutputStream(gitDirectory.resolve("olympusnews").resolve(news.slug() + ".md"))) {
            news.writeTo(os);
        }
    }

    public static void archiveOlympusNews(OlympusNews news) throws IOException {
        log.info("Archiving Olympus news: {}", news);

        if (news.image() != null) {
            Files.move(
                    gitDirectory.resolve("olympusnews").resolve(news.image()),
                    gitDirectory.resolve("olympusnews").resolve("archive").resolve(news.image())
            );
        }

        Files.move(
                gitDirectory.resolve("olympusnews").resolve(news.slug() + ".md"),
                gitDirectory.resolve("olympusnews").resolve("archive").resolve(news.slug() + ".md")
        );
    }

    public static void commitChanges() throws IOException {
        try {
            log.info("Adding");
            gitRepository.add()
                    .addFilepattern("olympusnews")
                    .call();

            log.info("Committing");
            gitRepository.commit()
                    .setAll(true)
                    .setAuthor("Nyan-Games", "24738390+nyan-games@users.noreply.github.com")
                    .setCommitter("maddie480", "52103563+maddie480@users.noreply.github.com")
                    .setMessage("[Olympus News Manager Discord bot] Update Olympus news")
                    .call();

            log.info("Pushing");
            gitRepository.push().call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }
}
