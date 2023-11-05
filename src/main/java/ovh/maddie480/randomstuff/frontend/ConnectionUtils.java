package ovh.maddie480.randomstuff.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class ConnectionUtils {
    /**
     * Creates an HttpURLConnection to the specified URL, getting sure timeouts are set
     * (connect timeout = 10 seconds, read timeout = 30 seconds).
     *
     * @param url The URL to connect to
     * @return An HttpURLConnection to this URL
     * @throws IOException If an exception occured while trying to connect
     */
    public static HttpURLConnection openConnectionWithTimeout(String url) throws IOException {
        URLConnection con;

        try {
            con = new URI(url).toURL().openConnection();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        con.setRequestProperty("User-Agent", "Maddie-Random-Stuff-Frontend/1.0.0 (+https://github.com/maddie480/RandomStuffWebsite)");
        con.setRequestProperty("Accept-Encoding", "gzip");

        con.setConnectTimeout(10000);
        con.setReadTimeout(30000);

        return (HttpURLConnection) con;
    }

    /**
     * Turns an HTTP connection into an input stream, going through gzip decoding if necessary.
     *
     * @param con The connection
     * @return An input stream that reads from the connection
     * @throws IOException If an exception occured while trying to connect
     */
    public static InputStream connectionToInputStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        if ("gzip".equals(con.getContentEncoding())) {
            return new GZIPInputStream(is);
        }
        return is;
    }

    /**
     * Creates a stream to the specified URL, getting sure timeouts are set
     * (connect timeout = 10 seconds, read timeout = 30 seconds).
     *
     * @param url The URL to connect to
     * @return A stream to this URL
     * @throws IOException If an exception occured while trying to connect
     */
    public static InputStream openStreamWithTimeout(String url) throws IOException {
        return connectionToInputStream(openConnectionWithTimeout(url));
    }
}
