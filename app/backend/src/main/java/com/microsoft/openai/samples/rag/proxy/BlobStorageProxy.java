// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.proxy;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class is a proxy to the Blob storage client. It is responsible for:
 * - calling the API
 * - handling errors and retry strategy
 * - add monitoring points
 * - add circuit breaker with exponential backoff
 */
@Component
public class BlobStorageProxy {

    private final BlobContainerClient client;

    public BlobStorageProxy(
            @Value("${storage-account.service}") String storageAccountServiceName,
            @Value("${blob.container.name}") String containerName,
            TokenCredential tokenCredential) {

        String endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountServiceName);
        this.client =
                new BlobContainerClientBuilder()
                        .endpoint(endpoint)
                        .credential(tokenCredential)
                        .containerName(containerName)
                        .buildClient();
    }

    public byte[] getFileAsBytes(String fileName) throws IOException {
        var blobClient = client.getBlobClient(fileName);
        int dataSize = (int) blobClient.getProperties().getBlobSize();

        // There is no need to close ByteArrayOutputStream.
        // https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataSize);
        blobClient.downloadStream(outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Uploads a file to a specified folder (prefix) in the blob container.
     * @param folderName The folder (prefix) to upload to. Use empty string for root.
     * @param fileBytes The file content as a byte array.
     * @param fileName The name of the file to be saved.
     */
    public void uploadFileToFolder(String folderName, byte[] fileBytes, String fileName) {
        String prefix = (folderName == null || folderName.isEmpty()) ? "default" : (folderName.endsWith("/") ? folderName : folderName + "/");
        String blobName = prefix + fileName;
        var blobClient = client.getBlobClient(blobName);
        blobClient.upload(new java.io.ByteArrayInputStream(fileBytes), fileBytes.length, true);
    }

    /**
     * Lists all file names under the specified folder (prefix) in the blob container.
     * @param folderName The folder (prefix) to search under. Use empty string for root.
     * @param filenameOnly If true, returns only the filename with extension; otherwise, returns the full path. Defaults to true if null.
     * @return List of file names (blobs) under the folder.
     */
    public List<String> listFilesInFolder(String folderName, Boolean filenameOnly) {
        String prefix = folderName == null || folderName.isEmpty() ? "" : folderName.endsWith("/") ? folderName : folderName + "/";
        List<String> fileNames = new ArrayList<>();
        boolean onlyName = (filenameOnly == null) ? true : filenameOnly;
        client.listBlobsByHierarchy(prefix).forEach(blobItem -> {
            if (!blobItem.isPrefix()) {
                String name = blobItem.getName();
                if (onlyName) {
                    int lastSlash = name.lastIndexOf('/');
                    if (lastSlash >= 0 && lastSlash < name.length() - 1) {
                        name = name.substring(lastSlash + 1);
                    }
                }
                fileNames.add(name);
            }
        });
        return fileNames;
    }

    /**
     * Deletes a file from the specified folder (prefix) in the blob container.
     * @param folderName The folder (prefix) where the file is located. Use empty string for root.
     * @param fileName The name of the file to be deleted.
     */
    public Boolean deleteIfExistsFileFromFolder(String folderName, String fileName) {
        String prefix = (folderName == null || folderName.isEmpty()) ? "default" : (folderName.endsWith("/") ? folderName : folderName + "/");
        String blobName = prefix + fileName;
        var blobClient = client.getBlobClient(blobName);
        return blobClient.deleteIfExists();
    }

    public Boolean exists(String folderName, String fileName) {
        String prefix = (folderName == null || folderName.isEmpty()) ? "default" : (folderName.endsWith("/") ? folderName : folderName + "/");
        String blobName = prefix + fileName;
        var blobClient = client.getBlobClient(blobName);
        return blobClient.exists();
    }


}
