package com.max480.randomstuff.gae;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemHeaders;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.UUID;

/**
 * A {@link FileItem} that uploads files straight to Cloud Storage.
 */
public class CloudStorageUploadItem implements FileItem {
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    private String fieldName;
    private final String contentType;
    private boolean isFormField;
    private final String fileName;
    private final BlobId cloudStorageFile;
    private FileItemHeaders headers;

    public CloudStorageUploadItem(String fieldName, String contentType, boolean isFormField, String fileName) {
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;

        cloudStorageFile = BlobId.of("staging.max480-random-stuff.appspot.com", "upload-" + UUID.randomUUID() + ".bin");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Channels.newInputStream(storage.reader(cloudStorageFile));
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public long getSize() {
        return storage.get(cloudStorageFile).getSize();
    }

    @Override
    public byte[] get() {
        return storage.readAllBytes(cloudStorageFile);
    }

    @Override
    public String getString(String encoding) throws UnsupportedEncodingException {
        return new String(get(), encoding);
    }

    @Override
    public String getString() {
        return new String(get());
    }

    @Override
    public void write(File file) throws Exception {
        try (ReadChannel reader = storage.reader(cloudStorageFile);
             FileOutputStream writerStream = new FileOutputStream(file);
             FileChannel writer = writerStream.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);
            while (reader.read(buffer) > 0 || buffer.position() != 0) {
                buffer.flip();
                writer.write(buffer);
                buffer.compact();
            }
        }
    }

    @Override
    public void delete() {
        storage.delete(cloudStorageFile);
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void setFieldName(String name) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean isFormField() {
        return isFormField;
    }

    @Override
    public void setFormField(boolean state) {
        isFormField = state;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return Channels.newOutputStream(storage.writer(BlobInfo.newBuilder(cloudStorageFile).setContentType(contentType).build()));
    }

    @Override
    public FileItemHeaders getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(FileItemHeaders headers) {
        this.headers = headers;
    }

    public BlobId getCloudStorageFile() {
        return cloudStorageFile;
    }

    public static class Factory implements FileItemFactory {
        @Override
        public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
            return new CloudStorageUploadItem(fieldName, contentType, isFormField, fileName);
        }
    }
}
