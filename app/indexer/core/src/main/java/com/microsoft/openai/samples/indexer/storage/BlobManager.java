package com.microsoft.openai.samples.indexer.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.*;

public class BlobManager {
    private String endpoint;
    private String container;
    private boolean verbose;
    private BlobServiceClient blobServiceClient;

    private BlobContainerClient blobcontainerClient;

    public BlobManager(String storageAccountName, String container, TokenCredential tokenCredential, boolean verbose) {
        this.endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountName);
        this.container = container;
        this.verbose = verbose;
        this.blobServiceClient = new BlobServiceClientBuilder().endpoint(endpoint).credential(tokenCredential).buildClient();
        this.blobcontainerClient =   new BlobContainerClientBuilder().endpoint(endpoint).credential(tokenCredential).containerName(container).buildClient();
    }

    public BlobManager(String storageAccountName, String container, TokenCredential tokenCredential, boolean verbose, BlobServiceClient blobServiceClient) {
        this.endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountName);
        this.container = container;
        this.verbose = verbose;
        this.blobServiceClient = blobServiceClient;
        this.blobcontainerClient =   new BlobContainerClientBuilder().endpoint(endpoint).credential(tokenCredential).containerName(container).buildClient();
    }

    public byte[] getFileAsBytes(String fileName) throws IOException {
        var blobClient = blobcontainerClient.getBlobClient(fileName);
        int dataSize = (int) blobClient.getProperties().getBlobSize();

        // There is no need to close ByteArrayOutputStream.
        // https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dataSize);
        blobClient.downloadStream(outputStream);

        return outputStream.toByteArray();
    }
    
    public void uploadBlob(File file) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);

        if (!containerClient.exists()) {
            containerClient.create();
        }

        String blobName = file.getName();
        System.out.println("\tUploading blob for whole file -> " + blobName);

        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(new FileInputStream(file), file.length(), true);
    }

    public void removeBlob(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Blob File Path cannot be null or empty");
        }
        
        if (!blobcontainerClient.exists()) {
            return;
        }

                
        File file = new File(path);
        String prefix = file.getName().split("\\.")[0];
        Pattern pattern = Pattern.compile(prefix + "-\\d+\\.pdf");


        blobcontainerClient.listBlobs().forEach(blobItem -> {
            String blobPath = blobItem.getName();

            if (prefix != null && !pattern.matcher(blobPath).matches()) {
                return;
            }

            if (path != null && blobPath.equals(new File(path).getName())) {
                return;
            }

            if (verbose) {
                System.out.println("\tRemoving blob " + blobPath);
            }

            blobcontainerClient.getBlobClient(blobPath).delete();
        });
    }

 
}
