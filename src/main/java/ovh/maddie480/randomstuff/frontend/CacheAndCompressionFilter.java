package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class CacheAndCompressionFilter extends HttpFilter {
    private interface CacheStream {
        int getStatus();

        String getETag() throws IOException;

        long getLength() throws IOException;

        InputStream getInputStream() throws IOException;
    }

    private static Object compressCacheLock = new Object();
    private static final Path cacheRoot;
    static {
        Path prodOne = Paths.get("/shared/temp/frontend-cache-compression-filter");
        Path devOne = Paths.get("/tmp");
        cacheRoot = Files.isDirectory(prodOne) ? prodOne : devOne;
    }

    private static void sendResponse(HttpServletRequest request, HttpServletResponse response, CacheStream content, boolean compress) throws IOException {
        try (InputStream source = content.getInputStream()) {
            long length = content.getLength();
            Path sourceFile = null;

            if (compress) {
                response.setHeader("Content-Encoding", "gzip");

                String filename = "gzip_" + URLEncoder.encode(request.getRequestURI(), StandardCharsets.UTF_8) + "_" + content.getETag() + ".bin.gz";
                if (filename.getBytes(StandardCharsets.UTF_8).length > 255) {
                    // file name too long, hash it
                    filename = "gzipsha_" + DigestUtils.sha256Hex(request.getRequestURI()) + "_" + content.getETag() + ".bin.gz";
                }

                Path compressCache = cacheRoot.resolve(filename);
                synchronized (compressCacheLock) {
                    if (!Files.exists(compressCache)) {
                        // compress the response right now
                        try (OutputStream fos = Files.newOutputStream(compressCache);
                             GZIPOutputStream os = new GZIPOutputStream(fos)) {

                            IOUtils.copy(source, os);
                        }
                    }
                }

                // we will send the compressed file instead of the original stream
                length = Files.size(compressCache);
                sourceFile = compressCache;
                Files.setLastModifiedTime(compressCache, FileTime.from(Instant.now()));
            }

            response.setContentLength((int) length);
            if (sourceFile == null) {
                IOUtils.copyLarge(source, response.getOutputStream());
            } else {
                try (InputStream is = Files.newInputStream(sourceFile)) {
                    IOUtils.copyLarge(is, response.getOutputStream());
                }
            }
        }
    }

    private static class ServletOutputStreamWrapper extends ServletOutputStream {
        private final OutputStream wrappedOutputStream;

        public ServletOutputStreamWrapper(OutputStream wrappedOutputStream) {
            this.wrappedOutputStream = wrappedOutputStream;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            throw new NotImplementedException();
        }

        @Override
        public void write(int b) throws IOException {
            wrappedOutputStream.write(b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            wrappedOutputStream.close();
        }
    }

    static class CachingServletResponse extends HttpServletResponseWrapper implements CacheStream, Closeable {
        PrintWriter writer = null;
        ServletOutputStream outputStream = null;

        private final Path writerTarget;
        private final Path outputStreamTarget;

        private static final Object rollLock = new Object();
        public String directFileToSend = null;

        public CachingServletResponse(HttpServletResponse response) throws IOException {
            super(response);

            synchronized (rollLock) {
                Path w, o;
                do {
                    // just be extra mega sure we don't have conflicts by rerolling if we do
                    double roll = Math.random();
                    w = cacheRoot.resolve("buffer_" + System.currentTimeMillis() + "_" + roll + ".writer.bin");
                    o = cacheRoot.resolve("buffer_" + System.currentTimeMillis() + "_" + roll + ".stream.bin");
                } while (Files.exists(w));

                writerTarget = w;
                outputStreamTarget = o;

                Files.createFile(w);
                Files.createFile(o);
            }
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            PrintWriter w = new PrintWriter(Files.newBufferedWriter(writerTarget, StandardCharsets.UTF_8));
            writer = w;
            return w;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            ServletOutputStream s = new ServletOutputStreamWrapper(new BufferedOutputStream(Files.newOutputStream(outputStreamTarget)));
            outputStream = s;
            return s;
        }

        public void finalizeRequest() throws IOException {
            if (writer != null) writer.close();
            if (outputStream != null) outputStream.close();
        }

        public String getETag() throws IOException {
            try (InputStream is = getInputStream()) {
                return DigestUtils.sha512Hex(is);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (Files.size(writerTarget) > Files.size(outputStreamTarget)) {
                return Files.newInputStream(writerTarget);
            } else {
                return Files.newInputStream(outputStreamTarget);
            }
        }

        @Override
        public long getLength() throws IOException {
            return Math.max(Files.size(writerTarget), Files.size(outputStreamTarget));
        }

        public void close() throws IOException {
            synchronized (rollLock) {
                Files.delete(writerTarget);
                Files.delete(outputStreamTarget);
            }
        }
    }

    private static class DirectFileSendResponse implements CacheStream {
        private static final Map<String, String> etagCache = new HashMap<>();
        private static final Map<String, Long> sizeCache = new HashMap<>();
        private final String path;

        private DirectFileSendResponse(String path) {
            this.path = path;
        }

        @Override
        public int getStatus() {
            return 200;
        }

        @Override
        public String getETag() throws IOException {
            synchronized (etagCache) {
                if (etagCache.containsKey(path)) {
                    return etagCache.get(path);
                }
            }

            String hash;
            try (InputStream is = getInputStream()) {
                hash = DigestUtils.sha512Hex(is);
            }
            synchronized (etagCache) {
                etagCache.put(path, hash);
            }
            return hash;
        }

        @Override
        public long getLength() throws IOException {
            synchronized (sizeCache) {
                if (sizeCache.containsKey(path)) {
                    return sizeCache.get(path);
                }
            }

            long size;
            try (InputStream is = getInputStream()) {
                size = IOUtils.consume(is);
            }
            synchronized (sizeCache) {
                sizeCache.put(path, size);
            }
            return size;
        }

        @Override
        public InputStream getInputStream() {
            return CacheAndCompressionFilter.class.getClassLoader().getResourceAsStream(path);
        }
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req.getRequestURI().equals("/celeste/bundle-download")) {
            // this one is smoother if it stays streamed
            chain.doFilter(req, res);
            return;
        }

        // only compress the output if the requester said they supported it, and if it isn't on a format that isn't worth compressing
        boolean compress = StaticAssetsAndRouteNotFoundServlet.FORMATS_NOT_WORTH_COMPRESSING.stream().noneMatch(req.getRequestURI()::endsWith)
                && req.getHeader("Accept-Encoding") != null
                && req.getHeader("Accept-Encoding").matches(".*(^|[, ])gzip($|[ ;,]).*");

        try (CachingServletResponse placeholderResponse = new CachingServletResponse(res)) {
            chain.doFilter(req, placeholderResponse);
            placeholderResponse.finalizeRequest();

            if (compress && req.getRequestURI().startsWith("/celeste/asset-drive/files/") && "image/png".equals(placeholderResponse.getContentType())) {
                // they're not ending with .png, but they're still PNGs and there's no point in trying to compress them
                compress = false;
            }

            CacheStream content = placeholderResponse;
            if (placeholderResponse.directFileToSend != null) {
                content = new DirectFileSendResponse(placeholderResponse.directFileToSend);
            }

            if (content.getStatus() != 200) {
                // no caching on failure responses
                sendResponse(req, res, content, compress);
                return;
            }

            String etag = content.getETag();
            if (etag.equals(req.getHeader("If-None-Match"))) {
                res.setStatus(304); // Not Modified
                res.setContentLength(0);
            } else {
                res.setHeader("ETag", etag);
                sendResponse(req, res, content, compress);
            }
        }
    }
}
