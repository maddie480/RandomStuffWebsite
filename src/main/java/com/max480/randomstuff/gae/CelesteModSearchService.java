package com.max480.randomstuff.gae;

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
import org.yaml.snakeyaml.Yaml;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet(name = "CelesteModSearchService", loadOnStartup = 1, urlPatterns = {"/celeste/gamebanana-search", "/celeste/gamebanana-search-reload"})
public class CelesteModSearchService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModSearchService");

    private final Analyzer analyzer = new StandardAnalyzer();
    private Directory modIndexDirectory = null;

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

        if (request.getParameter("q") == null || request.getParameter("q").trim().isEmpty()) {
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
                    query = new QueryParser("name", analyzer).parse(request.getParameter("q"));
                    logger.fine("Query we are going to run: " + query.toString());
                } catch (ParseException e) {
                    // query could not be parsed! aaaaa
                    // we will give up on trying to parse it and just interpret everything as search terms.
                    logger.info("Query could not be parsed!");
                    query = new QueryBuilder(analyzer).createBooleanQuery("name", request.getParameter("q"));

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

    // mapping takes an awful amount of time on App Engine (~2 seconds) so we can't make it when the user calls the API.
    private void refreshModDatabase() throws IOException {
        try (InputStream connectionToDatabase = new URL(Constants.MOD_SEARCH_DATABASE_URL).openStream()) {
            // download the mods
            List<HashMap<String, Object>> mods = new Yaml().load(connectionToDatabase);
            logger.fine("There are " + mods.size() + " mods in the search database.");

            RAMDirectory newDirectory = new RAMDirectory(); // I know it's deprecated but creating a directory on App Engine is weird

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
                }
            }

            modIndexDirectory = newDirectory;
            logger.fine("Virtual index directory contains " + modIndexDirectory.listAll().length + " files and uses "
                    + newDirectory.ramBytesUsed() + " bytes of RAM.");
        }
    }
}
