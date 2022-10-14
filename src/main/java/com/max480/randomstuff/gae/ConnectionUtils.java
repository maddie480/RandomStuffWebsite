package com.max480.randomstuff.gae;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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
        URLConnection con = new URL(url).openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(30000);
        return (HttpURLConnection) con;
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
        return openConnectionWithTimeout(url).getInputStream();
    }
}
