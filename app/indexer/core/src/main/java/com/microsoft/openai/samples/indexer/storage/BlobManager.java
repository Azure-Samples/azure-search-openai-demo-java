package com.microsoft.openai.samples.indexer.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

public class BlobManager {
    private String endpoint;
    private String container;
    private TokenCredential credential;
    private boolean verbose;
    private BlobServiceClient blobServiceClient;

    public BlobManager(String storageAccountName, String container, TokenCredential credential, boolean verbose) {
        this.endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountName);
        this.container = container;
        this.credential = credential;
        this.verbose = verbose;
        this.blobServiceClient = new BlobServiceClientBuilder().endpoint(endpoint).credential(credential).buildClient();
    }

    public BlobManager(String storageAccountName, String container, TokenCredential credential, boolean verbose, BlobServiceClient blobServiceClient) {
        this.endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountName);
        this.container = container;
        this.credential = credential;
        this.verbose = verbose;
        this.blobServiceClient = blobServiceClient;
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

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().endpoint(endpoint).credential(credential).buildClient();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
        
        if (!containerClient.exists()) {
            return;
        }

                
        File file = new File(path);
        String prefix = file.getName().split("\\.")[0];
        Pattern pattern = Pattern.compile(prefix + "-\\d+\\.pdf");
        

        containerClient.listBlobs().forEach(blobItem -> {
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

            containerClient.getBlobClient(blobPath).delete();
        });
    }

 
}
