package com.max480.randomstuff.gae;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A service that takes multiple YouTube channel names, and aggregate their Atom feeds into one.
 * This is a private service.
 */
@WebServlet(name = "YouTubeFeedAggregator", urlPatterns = {"/youtube-feed-aggregator"})
public class YouTubeFeedAggregator extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(YouTubeFeedAggregator.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String expectedQueryStringPrefix = "key=" + SecretConstants.RSS_AGGREGATOR_SECRET + "&channels=";

        if (request.getQueryString() == null || !request.getQueryString().startsWith(expectedQueryStringPrefix)) {
            // invalid secret
            log.warn("Invalid key");
            response.setStatus(403);
            return;
        }

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            // root element
            Element rootElement = doc.createElement("feed");
            doc.appendChild(rootElement);

            { // namespace attributes that make the XML actually valid
                Attr attr = doc.createAttribute("xmlns:yt");
                attr.setValue("http://www.youtube.com/xml/schemas/2015");
                rootElement.setAttributeNode(attr);

                attr = doc.createAttribute("xmlns:media");
                attr.setValue("http://search.yahoo.com/mrss/");
                rootElement.setAttributeNode(attr);

                attr = doc.createAttribute("xmlns");
                attr.setValue("http://www.w3.org/2005/Atom");
                rootElement.setAttributeNode(attr);
            }

            // title tag
            Element element = doc.createElement("title");
            element.appendChild(doc.createTextNode("Aggregated YouTube feeds"));
            rootElement.appendChild(element);

            // load all elements
            Arrays.stream(request.getQueryString().substring(expectedQueryStringPrefix.length()).split(","))
                    .parallel()
                    .map(channelId -> {
                        try (InputStream is = ConnectionUtils.openStreamWithTimeout("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId)) {
                            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                            Document document = documentBuilder.parse(is);

                            return document.getElementsByTagName("entry");
                        } catch (ParserConfigurationException | IOException | SAXException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(nodeList -> {
                        synchronized (doc) {
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                Node node = nodeList.item(i);
                                doc.adoptNode(node); // yes, it is illegal to add a child to a document before it gets adopted, apparently.
                                rootElement.appendChild(node);
                            }
                        }
                    });

            // write the content
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            response.setContentType("application/atom+xml");
            StreamResult result = new StreamResult(response.getOutputStream());
            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException(e);
        }
    }
}
