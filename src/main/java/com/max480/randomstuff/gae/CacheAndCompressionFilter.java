package com.max480.randomstuff.gae;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.annotation.WebFilter;
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

@WebFilter(filterName = "CacheEtagFilter", urlPatterns = "/*")
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
        if (req.getRequestURI().equals("/celeste/bundle-download")) {
            // this one MUST be streamed to the client
            chain.doFilter(req, res);
            return;
        }

        boolean compress = req.getHeader("Accept-Encoding") != null && req.getHeader("Accept-Encoding").matches(".*(^|[, ])gzip($|[ ;,]).*");

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
        } else {
            placeholderResponse.setHeader("ETag", etag);
            placeholderResponse.sendResponse();
        }
    }
}
