package com.max480.randomstuff.gae;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class CloudStorageUtils {
    private static final Storage storage = StorageOptions.newBuilder().setProjectId("max480-random-stuff").build().getService();

    public static InputStream getCloudStorageInputStream(String filename) {
        BlobId blobId = BlobId.of("max480-random-stuff.appspot.com", filename);
        return new ByteArrayInputStream(storage.readAllBytes(blobId));
    }

    public static void sendBytesToCloudStorage(String filename, String contentType, byte[] contents) {
        BlobId blobId = BlobId.of("max480-random-stuff.appspot.com", filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setCacheControl("no-store")
                .build();
        storage.create(blobInfo, contents);
    }
}
