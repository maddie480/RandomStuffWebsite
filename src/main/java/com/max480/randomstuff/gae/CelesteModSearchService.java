package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
import org.jsoup.Jsoup;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This servlet provides the GameBanana search API, and other APIs that are used by Olympus or the Banana Mirror Browser.
 */
@WebServlet(name = "CelesteModSearchService", loadOnStartup = 2, urlPatterns = {"/celeste/gamebanana-search",
        "/celeste/gamebanana-search-reload", "/celeste/gamebanana-list", "/celeste/gamebanana-categories", "/celeste/webp-to-png",
        "/celeste/banana-mirror-image", "/celeste/mod_search_database.yaml"})
public class CelesteModSearchService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModSearchService");

    private final Analyzer analyzer = new StandardAnalyzer();
    private Directory modIndexDirectory = null;
    private List<ModInfo> modDatabaseForSorting = Collections.emptyList();
    private Map<Integer, String> modCategories;

    private static class ModInfo {
        public final String type;
        public final int id;
        public final int likes;
        public final int views;
        public final int downloads;
        public final int categoryId;
        public final int createdDate;

        private ModInfo(String type, int id, int likes, int views, int downloads, int categoryId, int createdDate) {
            this.type = type;
            this.id = id;
            this.likes = likes;
            this.views = views;
            this.downloads = downloads;
            this.categoryId = categoryId;
            this.createdDate = createdDate;
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
                    List<Map<String, Object>> responseBody;
                    responseBody = Arrays.stream(hits).map(hit -> {
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

        if (request.getRequestURI().equals("/celeste/mod_search_database.yaml")) {
            response.setHeader("Content-Type", "text/yaml");
            try (InputStream is = CelesteModUpdateService.getCloudStorageInputStream("mod_search_database.yaml")) {
                IOUtils.copy(is, response.getOutputStream());
            }
        }

        if (request.getRequestURI().equals("/celeste/gamebanana-list")) {
            String sortParam = request.getParameter("sort");
            String pageParam = request.getParameter("page");
            String typeParam = request.getParameter("type") == null ? request.getParameter("itemtype") : request.getParameter("type");
            String categoryParam = request.getParameter("category");

            if (!Arrays.asList("latest", "likes", "views", "downloads").contains(sortParam)) {
                // invalid sort!
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(400);
                response.getWriter().write("expected \"sort\" parameter with value \"latest\", \"likes\", \"views\" or \"downloads\"");
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

                // is there a type and/or a category filter?
                Predicate<ModInfo> typeFilter = info -> true;
                if (typeParam != null) {
                    if (categoryParam != null) {
                        typeFilter = info -> typeParam.equalsIgnoreCase(info.type) && Integer.toString(info.categoryId).equals(categoryParam);
                    } else {
                        typeFilter = info -> typeParam.equalsIgnoreCase(info.type);
                    }
                } else if (categoryParam != null) {
                    typeFilter = info -> Integer.toString(info.categoryId).equals(categoryParam);
                }

                // determine the field on which we want to sort. Sort by descending id to tell equal values apart.
                Comparator<ModInfo> sort;
                switch (sortParam) {
                    case "views":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.views).thenComparingInt(i -> -i.id);
                        break;
                    case "likes":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.likes).thenComparingInt(i -> -i.id);
                        break;
                    case "downloads":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.downloads).thenComparingInt(i -> -i.id);
                        break;
                    case "latest":
                        sort = Comparator.<ModInfo>comparingInt(i -> -i.createdDate).thenComparingInt(i -> -i.id);
                        break;
                    default:
                        sort = null;
                        break;
                }

                // then sort on it.
                Stream<ModInfo> responseBodyStream = modDatabaseForSorting.stream()
                        .filter(typeFilter);

                if (sort != null) {
                    responseBodyStream = responseBodyStream.sorted(sort);
                }

                final List<Map<String, Object>> responseBody = responseBodyStream
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
            boolean v2 = "2".equals(request.getParameter("version"));

            // go across all mods and aggregate stats per category.
            HashMap<Object, Integer> categoriesAndCounts = new HashMap<>();
            for (ModInfo modInfo : modDatabaseForSorting) {
                Object category = modInfo.type;
                if (v2 && category.equals("Mod")) {
                    category = modInfo.categoryId;
                }
                if (!categoriesAndCounts.containsKey(category)) {
                    // first mod encountered in this category
                    categoriesAndCounts.put(category, 1);
                } else {
                    // add 1 to the mod count in the category
                    categoriesAndCounts.put(category, categoriesAndCounts.get(category) + 1);
                }
            }

            // format the map for the response...
            List<Map<String, Object>> categoriesList = categoriesAndCounts.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        if (entry.getKey() instanceof String) {
                            // itemtype
                            result.put("itemtype", entry.getKey());
                            result.put("formatted", formatGameBananaItemtype(entry.getKey().toString(), true));
                        } else {
                            // mod category
                            result.put("categoryid", entry.getKey());
                            result.put("formatted", modCategories.get(entry.getKey()));
                        }
                        result.put("count", entry.getValue());
                        return result;
                    })
                    .sorted(Comparator.comparing(result -> result.get("formatted").toString()))
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

        // "redirect to matching image on Banana Mirror" service, that also responds to /celeste/webp-to-png for backwards compatibility
        if (request.getRequestURI().equals("/celeste/webp-to-png") || request.getRequestURI().equals("/celeste/banana-mirror-image")) {
            String imagePath = request.getParameter("src");
            if (imagePath == null) {
                // no image path passed!
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(400);
                response.getWriter().write("expected \"src\" parameter");
            } else if ((!imagePath.startsWith("https://screenshots.gamebanana.com/") && !imagePath.startsWith("https://images.gamebanana.com/"))) {
                // the URL passed is not from GameBanana.
                logger.warning("Returned 403 after trying to use conversion with non-GB URL");
                response.setHeader("Content-Type", "text/plain");
                response.setStatus(403);
                response.getWriter().write("this API can only be used with GameBanana");
            } else {
                // find out what the ID on the mirror is going to be, and redirect to it.
                String screenshotId;
                if (imagePath.startsWith("https://screenshots.gamebanana.com/")) {
                    screenshotId = imagePath.substring("https://screenshots.gamebanana.com/".length());
                } else {
                    screenshotId = imagePath.substring("https://images.gamebanana.com/".length());
                }
                screenshotId = screenshotId.substring(0, screenshotId.lastIndexOf(".")).replace("/", "_") + ".png";

                if (request.getRequestURI().equals("/celeste/webp-to-png")) {
                    // for compatibility, remove the 220-90 prefix.
                    screenshotId = screenshotId.replace("220-90_", "");
                }

                response.setStatus(302);
                response.setHeader("Location", "https://celestemodupdater.0x0a.de/banana-mirror-images/" + screenshotId);
            }
        }
    }

    public static String formatGameBananaItemtype(String input, boolean pluralize) {
        // specific formatting for a few categories
        if (input.equals("Gamefile")) {
            return pluralize ? "Game files" : "Game file";
        } else if (input.equals("Wip")) {
            return pluralize ? "WiPs" : "WiP";
        } else if (input.equals("Gui")) {
            return pluralize ? "GUIs" : "GUI";
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

        if (!pluralize) {
            return result;
        }

        // pluralize
        if (result.charAt(result.length() - 1) == 'y') {
            return result.substring(0, result.length() - 1) + "ies";
        }
        return result + "s";
    }

    // mapping takes an awful amount of time on App Engine (~2 seconds) so we can't make it when the user calls the API.
    private void refreshModDatabase() throws IOException {
        try (InputStream connectionToDatabase = CelesteModUpdateService.getCloudStorageInputStream("mod_search_database.yaml")) {
            // download the mods
            List<HashMap<String, Object>> mods = new Yaml().load(connectionToDatabase);
            logger.fine("There are " + mods.size() + " mods in the search database.");

            RAMDirectory newDirectory = new RAMDirectory(); // I know it's deprecated but creating a directory on App Engine is weird
            List<ModInfo> newModDatabaseForSorting = new LinkedList<>();
            Map<Integer, String> newModCategories = new HashMap<>();

            // feed the mods to Lucene so that it indexes them
            try (IndexWriter index = new IndexWriter(newDirectory, new IndexWriterConfig(analyzer))) {
                for (HashMap<String, Object> mod : mods) {
                    int categoryId = -1;

                    Document modDocument = new Document();
                    modDocument.add(new TextField("type", mod.get("GameBananaType").toString(), Field.Store.YES));
                    modDocument.add(new TextField("id", mod.get("GameBananaId").toString(), Field.Store.YES));
                    modDocument.add(new TextField("name", mod.get("Name").toString(), Field.Store.YES));
                    modDocument.add(new TextField("author", mod.get("Author").toString(), Field.Store.NO));
                    modDocument.add(new TextField("summary", mod.get("Description").toString(), Field.Store.NO));
                    modDocument.add(new TextField("description", Jsoup.parseBodyFragment(mod.get("Text").toString()).text(), Field.Store.NO));
                    if (mod.get("CategoryName") != null) {
                        modDocument.add(new TextField("category", mod.get("CategoryName").toString(), Field.Store.NO));

                        categoryId = (int) mod.get("CategoryId");
                        newModCategories.put(categoryId, mod.get("CategoryName").toString());
                    }
                    index.addDocument(modDocument);

                    newModDatabaseForSorting.add(new ModInfo(mod.get("GameBananaType").toString(), (int) mod.get("GameBananaId"),
                            (int) mod.get("Likes"), (int) mod.get("Views"), (int) mod.get("Downloads"), categoryId, (int) mod.get("CreatedDate")));
                }
            }

            modIndexDirectory = newDirectory;
            modDatabaseForSorting = newModDatabaseForSorting;
            modCategories = newModCategories;

            logger.fine("Virtual index directory contains " + modIndexDirectory.listAll().length + " files and uses "
                    + newDirectory.ramBytesUsed() + " bytes of RAM.");
        }
    }
}
