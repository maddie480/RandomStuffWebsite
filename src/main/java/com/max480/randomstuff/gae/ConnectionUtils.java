package com.max480.randomstuff.gae;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ConnectionUtils {
    /**
     * Opens a stream to the specified URL, getting sure timeouts are set
     * (connect timeout = 10 seconds, read timeout = 30 seconds).
     *
     * @param url The URL to connect to
     * @return A stream to this URL
     * @throws IOException If an exception occured while trying to connect
     */
    public static InputStream openStreamWithTimeout(URL url) throws IOException {
        URLConnection con = url.openConnection();
        con.setConnectTimeout(10000);
        con.setReadTimeout(30000);
        return con.getInputStream();
    }
}
