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
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class CacheAndCompressionFilter extends HttpFilter {
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

        public OutputStream getWrappedOutputStream() {
            return wrappedOutputStream;
        }
    }

    private static class CachingServletResponse extends HttpServletResponseWrapper {
        private final HttpServletResponse wrappedResponse;
        private final boolean compress;

        private StringWriter writer;
        private ByteArrayOutputStream outputStream;

        public CachingServletResponse(HttpServletResponse response, boolean compress) {
            super(response);
            this.wrappedResponse = response;
            this.compress = compress;
        }

        @Override
        public PrintWriter getWriter() {
            writer = new StringWriter();
            return new PrintWriter(writer);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            outputStream = new ByteArrayOutputStream();
            return new ServletOutputStreamWrapper(outputStream);
        }

        public String getETag() {
            if (writer != null) {
                return "\"" + DigestUtils.sha512Hex(writer.toString().getBytes(StandardCharsets.UTF_8)) + "\"";
            } else if (outputStream != null) {
                return "\"" + DigestUtils.sha512Hex(outputStream.toByteArray()) + "\"";
            } else {
                return null;
            }
        }

        public void sendResponse() throws IOException {
            if (writer == null && outputStream == null) {
                // nothing to do /shrug
                return;
            }

            ByteArrayOutputStream finalContent = new ByteArrayOutputStream();

            if (compress) {
                wrappedResponse.setHeader("Content-Encoding", "gzip");

                try (GZIPOutputStream os = new GZIPOutputStream(finalContent)) {
                    if (writer != null) {
                        wrappedResponse.setCharacterEncoding("UTF-8");
                        IOUtils.write(writer.toString(), os, StandardCharsets.UTF_8);
                    } else {
                        IOUtils.write(outputStream.toByteArray(), os);
                    }
                }
            } else {
                if (writer != null) {
                    wrappedResponse.setCharacterEncoding("UTF-8");
                    IOUtils.write(writer.toString(), finalContent, StandardCharsets.UTF_8);
                } else {
                    finalContent = outputStream;
                }
            }

            byte[] content = finalContent.toByteArray();
            wrappedResponse.setContentLength(content.length);
            wrappedResponse.getOutputStream().write(content);
        }
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req.getRequestURI().equals("/celeste/bundle-download")
                || req.getRequestURI().equals("/static/graphics-dump.zip")
                || req.getRequestURI().equals("/static/celeste-fmod-project.zip")) {

            // big media; we shouldn't try compressing or caching it
            chain.doFilter(req, res);
            return;
        }

        // only compress the output if the requester said they supported it, and if it isn't on a format that isn't worth compressing
        boolean compress = StaticAssetsAndRouteNotFoundServlet.FORMATS_NOT_WORTH_COMPRESSING.stream().noneMatch(req.getRequestURI()::endsWith)
                && req.getHeader("Accept-Encoding") != null
                && req.getHeader("Accept-Encoding").matches(".*(^|[, ])gzip($|[ ;,]).*");

        CachingServletResponse placeholderResponse = new CachingServletResponse(res, compress);
        chain.doFilter(req, placeholderResponse);

        if (placeholderResponse.getStatus() != 200) {
            // no caching on failure responses
            placeholderResponse.sendResponse();
            return;
        }

        String etag = placeholderResponse.getETag();
        if (etag == null) {
            // no body was written, so nothing to cache
            placeholderResponse.sendResponse();
            return;
        }

        if (etag.equals(req.getHeader("If-None-Match"))) {
            placeholderResponse.setStatus(304); // Not Modified
            placeholderResponse.setContentLength(0);
        } else {
            placeholderResponse.setHeader("ETag", etag);
            placeholderResponse.sendResponse();
        }
    }
}
