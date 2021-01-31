package com.max480.randomstuff.gae;

import com.google.cloud.storage.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.QueryBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(name = "CelesteModSearchService", loadOnStartup = 1, urlPatterns = {"/celeste/gamebanana-search",
        "/celeste/gamebanana-search-reload", "/celeste/gamebanana-list", "/celeste/gamebanana-categories", "/celeste/webp-to-png"})
public class CelesteModSearchService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModSearchService");

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final Analyzer analyzer = new StandardAnalyzer();
    private Directory modIndexDirectory = null;
    private List<ModInfo> modDatabaseForSorting = Collections.emptyList();

    private final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    private static class ModInfo {
        public final String type;
        public final int id;
        public final int likes;
        public final int views;
        public final int downloads;

        private ModInfo(String type, int id, int likes, int views, int downloads) {
            this.type = type;
            this.id = id;
            this.likes = likes;
            this.views = views;
            this.downloads = downloads;
        }
    }

    @Override
    public void init() {
        try {
            refreshModDatabase();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Warming up failed: " + e.toString());
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/celeste/gamebanana-search-reload")) {
            if (("key=" + Constants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {
                refreshModDatabase();
            } else {
                // invalid secret
                response.setStatus(403);
            }
            return;
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-search")) {
            String queryParam = request.getParameter("q");

            if (queryParam == null || queryParam.trim().isEmpty()) {
                // the user didn't give any search!
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(400);
                response.getWriter().write("\"q\" query parameter expected");
            } else {
                // let's prepare a request through Lucene.
                try (DirectoryReader reader = DirectoryReader.open(modIndexDirectory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    Query query;
                    try {
                        // try parsing the query.
                        query = new QueryParser("name", analyzer).parse(queryParam);
                        logger.fine("Query we are going to run: " + query.toString());
                    } catch (ParseException e) {
                        // query could not be parsed! aaaaa
                        // we will give up on trying to parse it and just interpret everything as search terms.
                        logger.info("Query could not be parsed!");
                        query = new QueryBuilder(analyzer).createBooleanQuery("name", queryParam);

                        if (query == null) {
                            // invalid request is invalid! (for example "*")
                            logger.warning("Could not generate fallback request!");
                            response.setHeader("Content-Type", "text/yaml");
                            response.getWriter().write(new Yaml().dump(Collections.emptyList()));
                            return;
                        }

                        logger.fine("Fallback query we are going to run: " + query.toString());
                    }

                    ScoreDoc[] hits = searcher.search(query, 20).scoreDocs;

                    // convert the results to yaml
                    List<Map<String, Object>> responseBody = Arrays.stream(hits).map(hit -> {
                        try {
                            Document doc = searcher.doc(hit.doc);
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("itemtype", doc.get("type"));
                            result.put("itemid", Integer.parseInt(doc.get("id")));

                            logger.fine("Result: " + doc.get("type") + " " + doc.get("id")
                                    + " (" + doc.get("name") + ") with " + hit.score + " pt(s)");
                            return result;
                        } catch (IOException e) {
                            // how would we have an I/O exception on a memory stream?
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());

                    // send out the response
                    response.setHeader("Content-Type", "text/yaml");
                    response.getWriter().write(new Yaml().dump(responseBody));
                }
            }
        }


        if (request.getRequestURI().equals("/celeste/gamebanana-list")) {
            String sortParam = request.getParameter("sort");
            String pageParam = request.getParameter("page");
            String typeParam = request.getParameter("type");

            if (!Arrays.asList("likes", "views", "downloads").contains(sortParam)) {
                // invalid sort!
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(400);
                response.getWriter().write("expected \"sort\" parameter with value \"likes\", \"views\" or \"downloads\"");
            } else {
                // parse the page number: if page number is absent or invalid, assume 1
                int page = 1;
                if (pageParam != null) {
                    try {
                        page = Integer.parseInt(pageParam);
                    } catch (NumberFormatException e) {
                        logger.info("Invalid page number, assuming 1");
                    }
                }

                // is there a type filter?
                Predicate<ModInfo> typeFilter = info -> true;
                if (typeParam != null) {
                    typeFilter = info -> typeParam.equalsIgnoreCase(info.type);
                }

                // determine the field on which we want to sort. Sort by descending id to tell equal values apart.
                Comparator<ModInfo> sort;
                switch (sortParam) {
                    case "views":
                    default:
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.views).thenComparingInt(i -> -i.id);
                        break;
                    case "likes":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.likes).thenComparingInt(i -> -i.id);
                        break;
                    case "downloads":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.downloads).thenComparingInt(i -> -i.id);
                        break;
                }

                // then sort on it.
                List<Map<String, Object>> responseBody = modDatabaseForSorting.stream()
                        .filter(typeFilter)
                        .sorted(sort)
                        .skip((page - 1) * 20L)
                        .limit(20)
                        .map(modInfo -> {
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("itemtype", modInfo.type);
                            result.put("itemid", modInfo.id);
                            return result;
                        })
                        .collect(Collectors.toList());

                // send out the response.
                response.setHeader("Content-Type", "text/yaml");
                response.getWriter().write(new Yaml().dump(responseBody));
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-categories")) {
            // go across all mods and aggregate stats per category.
            TreeMap<String, Integer> categoriesAndCounts = new TreeMap<>();
            for (ModInfo modInfo : modDatabaseForSorting) {
                if (!categoriesAndCounts.containsKey(modInfo.type)) {
                    // first mod encountered in this category
                    categoriesAndCounts.put(modInfo.type, 1);
                } else {
                    // add 1 to the mod count in the category
                    categoriesAndCounts.put(modInfo.type, categoriesAndCounts.get(modInfo.type) + 1);
                }
            }

            // format the map for the response...
            List<Map<String, Object>> categoriesList = categoriesAndCounts.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("itemtype", entry.getKey());
                        result.put("formatted", formatGameBananaCategory(entry.getKey()));
                        result.put("count", entry.getValue());
                        return result;
                    })
                    .collect(Collectors.toList());

            // also add an "All" option to pass the total number of mods.
            Map<String, Object> all = new HashMap<>();
            all.put("itemtype", "");
            all.put("formatted", "All");
            all.put("count", modDatabaseForSorting.size());

            // the final list is "All" followed by all the categories.
            List<Map<String, Object>> responseBody = new ArrayList<>();
            responseBody.add(all);
            responseBody.addAll(categoriesList);

            // send out the response (the "block" flow style works better with Olympus).
            response.setHeader("Content-Type", "text/yaml");
            response.getWriter().write(new Yaml().dumpAs(responseBody, null, DumperOptions.FlowStyle.BLOCK));
        }


        if (request.getRequestURI().equals("/celeste/webp-to-png")) {
            String imagePath = request.getParameter("src");
            if (imagePath == null) {
                // no image path passed!
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(400);
                response.getWriter().write("expected \"src\" parameter");
            } else if (!imagePath.startsWith("https://screenshots.gamebanana.com/") || !imagePath.endsWith(".webp")) {
                // no image path passed!
                logger.warning("Returned 403 after trying to use conversion with non-GB URL");
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(403);
                response.getWriter().write("this API can only be used with GameBanana");
            } else {
                BlobId blobId = BlobId.of("max480-webp-to-png-cache", URLEncoder.encode(imagePath, "UTF-8") + ".png");
                Blob cachedImage = storage.get(blobId);
                if (cachedImage != null) {
                    // image was cached.
                    response.setHeader("Content-Type", "image/png");
                    IOUtils.write(cachedImage.getContent(), response.getOutputStream());
                } else {
                    // go get the image!
                    try (CloseableHttpResponse gamebananaResponse = httpClient.execute(new HttpGet(imagePath))) {
                        int status = gamebananaResponse.getStatusLine().getStatusCode();
                        if (status >= 400) {
                            // GameBanana responded with an error.
                            logger.info("GameBanana returned an error");
                            response.setHeader("Content-Type", "text/plain");
                            response.setStatus(status);
                            response.getWriter().write("GameBanana returned an error");
                        } else {
                            // read the image from GameBanana.
                            BufferedImage image = ImageIO.read(gamebananaResponse.getEntity().getContent());
                            try (ByteArrayOutputStream imageOutput = new ByteArrayOutputStream()) {
                                // write it as a PNG and cache it.
                                ImageIO.write(image, "png", imageOutput);
                                byte[] output = imageOutput.toByteArray();
                                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();
                                storage.create(blobInfo, output);

                                // send the response.
                                response.setHeader("Content-Type", "image/png");
                                IOUtils.write(output, response.getOutputStream());
                            }

                            logger.fine("Image was added to the cache.");
                        }
                    }
                }
            }
        }
    }

    public static String formatGameBananaCategory(String input) {
        // specific formatting for a few categories
        if (input.equals("Gamefile")) {
            return "Game files";
        } else if (input.equals("Wip")) {
            return "WiPs";
        } else if (input.equals("Gui")) {
            return "GUIs";
        } else if (input.equals("PositionAvailable")) {
            return "Positions Available";
        }

        // apply the spaced pascal case from Everest
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(input.charAt(i - 1)))
                builder.append(' ');

            if (i != 0 && builder.charAt(builder.length() - 1) == ' ') {
                builder.append(Character.toUpperCase(c));
            } else {
                builder.append(c);
            }
        }

        // capitalize the first letter
        String result = builder.toString();
        result = result.substring(0, 1).toUpperCase() + result.substring(1);

        // pluralize
        if (result.charAt(result.length() - 1) == 'y') {
            return result.substring(0, result.length() - 1) + "ies";
        }
        return result + "s";
    }

    // mapping takes an awful amount of time on App Engine (~2 seconds) so we can't make it when the user calls the API.
    private void refreshModDatabase() throws IOException {
        try (InputStream connectionToDatabase = new URL(Constants.MOD_SEARCH_DATABASE_URL).openStream()) {
            // download the mods
            List<HashMap<String, Object>> mods = new Yaml().load(connectionToDatabase);
            logger.fine("There are " + mods.size() + " mods in the search database.");

            RAMDirectory newDirectory = new RAMDirectory(); // I know it's deprecated but creating a directory on App Engine is weird
            List<ModInfo> newModDatabaseForSorting = new LinkedList<>();

            // feed the mods to Lucene so that it indexes them
            try (IndexWriter index = new IndexWriter(newDirectory, new IndexWriterConfig(analyzer))) {
                for (HashMap<String, Object> mod : mods) {
                    // this is a hack to give more weight to authors when they are on top of the credits list.
                    // first author has 20x weight, second has 10x, third has 5x and fourth has 2x.
                    int authorWeight = 20;
                    StringBuilder weightedAuthors = new StringBuilder();
                    for (String author : ((List<Object>) mod.get("Authors")).stream().map(Object::toString).collect(Collectors.toList())) {
                        for (int i = 0; i < authorWeight; i++) {
                            weightedAuthors.append(author).append(", ");
                        }
                        if (authorWeight > 1) authorWeight /= 2;
                    }
                    logger.finest("Weighted authors: " + weightedAuthors);

                    Document modDocument = new Document();
                    modDocument.add(new TextField("type", mod.get("GameBananaType").toString(), Field.Store.YES));
                    modDocument.add(new StoredField("id", mod.get("GameBananaId").toString()));
                    modDocument.add(new TextField("name", mod.get("Name").toString(), Field.Store.YES));
                    modDocument.add(new TextField("author", weightedAuthors.toString(), Field.Store.NO));
                    modDocument.add(new TextField("summary", mod.get("Description").toString(), Field.Store.NO));
                    modDocument.add(new TextField("description", mod.get("Text").toString(), Field.Store.NO));
                    index.addDocument(modDocument);

                    newModDatabaseForSorting.add(new ModInfo(mod.get("GameBananaType").toString(), (int) mod.get("GameBananaId"),
                            (int) mod.get("Likes"), (int) mod.get("Views"), (int) mod.get("Downloads")));
                }
            }

            modIndexDirectory = newDirectory;
            modDatabaseForSorting = newModDatabaseForSorting;

            logger.fine("Virtual index directory contains " + modIndexDirectory.listAll().length + " files and uses "
                    + newDirectory.ramBytesUsed() + " bytes of RAM.");
        }
    }
}
