package com.max480.randomstuff.gae;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A service that only returns the first article of a feed, and caches it for a given amount of seconds.
 * This allows to limit the output of a feed, even if some articles get skipped as a result.
 * This is a private service.
 */
@WebServlet(name = "FeedRateLimiter", urlPatterns = {"/rate-limited-feed"})
public class FeedRateLimiter extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(FeedRateLimiter.class);

    private static final Map<String, Pair<String, Instant>> cached = new HashMap<>();

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String expectedQueryStringPrefix = "key=" + SecretConstants.RSS_AGGREGATOR_SECRET + "&limit=";

        if (request.getQueryString() == null || !request.getQueryString().startsWith(expectedQueryStringPrefix)) {
            // invalid secret
            log.warn("Invalid key");
            response.setStatus(403);
            return;
        }

        response.setContentType("application/atom+xml");

        int expiration = Integer.parseInt(request.getQueryString().substring(expectedQueryStringPrefix.length(), request.getQueryString().indexOf("&feed=")));
        String feedUrl = request.getQueryString().substring(request.getQueryString().indexOf("&feed=") + 6);

        if (cached.containsKey(feedUrl) && cached.get(feedUrl).getRight().isAfter(Instant.now())) {
            response.getWriter().write(cached.get(feedUrl).getLeft());
            return;
        }

        // load the original feed
        HttpURLConnection connection = ConnectionUtils.openConnectionWithTimeout(feedUrl);
        connection.setRequestProperty("User-Agent", "linux:feedreader:v1.0.0 (by /u/max4805)");

        Document feed;

        try (InputStream is = connection.getInputStream()) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            feed = documentBuilder.parse(is);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new IOException(e);
        }

        // remove all the articles except the first
        NodeList entries = feed.getElementsByTagName("entry");
        for (int i = entries.getLength() - 1; i > 0; i--) {
            entries.item(i).getParentNode().removeChild(entries.item(i));
        }

        // write the content to a string
        String result;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(feed);
            transformer.transform(source, new StreamResult(os));

            result = os.toString(StandardCharsets.UTF_8);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }

        // cache and return it
        Instant cacheExpires = Instant.now().plusSeconds(expiration);
        cached.put(feedUrl, Pair.of(result, cacheExpires));
        log.info("Retrieved feed for {}, cached until {}", feedUrl, cacheExpires);
        response.getWriter().write(result);
    }
}
