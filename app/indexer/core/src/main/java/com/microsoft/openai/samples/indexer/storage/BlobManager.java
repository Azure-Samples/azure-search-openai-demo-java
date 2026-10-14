package com.microsoft.openai.samples.indexer.storage;

import java.io.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.credential.TokenCredential;
import com.azure.storage.blob.*;

public class BlobManager {
    private static final Logger logger = LoggerFactory.getLogger(BlobManager.class);
    
    private String endpoint;
    private String container;
    
    private BlobServiceClient blobServiceClient;

    private BlobContainerClient blobcontainerClient;

    public BlobManager(String storageAccountName, String container, TokenCredential tokenCredential) {
        this.endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountName);
        this.container = container;
        
        this.blobServiceClient = new BlobServiceClientBuilder().endpoint(endpoint).credential(tokenCredential).buildClient();
        this.blobcontainerClient =   new BlobContainerClientBuilder().endpoint(endpoint).credential(tokenCredential).containerName(container).buildClient();
    }

    public BlobManager(String storageAccountName, String container, TokenCredential tokenCredential, BlobServiceClient blobServiceClient) {
        this.endpoint = "https://%s.blob.core.windows.net".formatted(storageAccountName);
        this.container = container;
    
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

    /**
     * Uploads a file to a specified folder (prefix) in the blob container.
     * @param folderName The folder (prefix) to upload to. Use empty string for root.
     * @param file The file content
     */
    public void uploadFileToFolder(String folderName, File file) throws IOException{
        String prefix = (folderName == null || folderName.isEmpty()) ? "default/" : (folderName.endsWith("/") ? folderName : folderName + "/");
        String blobName = prefix + file.getName();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);
        var blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(new FileInputStream(file), file.length(), true);
    }
    
    public void uploadBlob(File file) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);

        if (!containerClient.exists()) {
            containerClient.create();
        }

        String blobName = file.getName();
        logger.info("Uploading blob for whole file -> {}", blobName);

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

            
            logger.debug("Removing blob {}", blobPath);
            

            blobcontainerClient.getBlobClient(blobPath).delete();
            logger.info("Blob {} removed successfully", blobPath);
        });
    }

    /**
     * Moves a file from one folder to another within the blob container.
     * @param fileName The name of the file to move (without any folder prefix)
     * @param sourceFolder The source folder (prefix). Use empty string or null for root.
     * @param destinationFolder The destination folder (prefix). Use empty string or null for root.
     * @throws IOException if the operation fails
     */
    public void moveFileToFolder(String fileName, String sourceFolder, String destinationFolder) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        // Normalize folder paths
        String sourcePrefix = (sourceFolder == null || sourceFolder.isEmpty()) ? "" : 
            (sourceFolder.endsWith("/") ? sourceFolder : sourceFolder + "/");
        String destinationPrefix = (destinationFolder == null || destinationFolder.isEmpty()) ? "" : 
            (destinationFolder.endsWith("/") ? destinationFolder : destinationFolder + "/");

        String sourceBlobName = sourcePrefix + fileName;
        String destinationBlobName = destinationPrefix + fileName;

        // Check if source blob exists
        BlobClient sourceBlobClient = blobcontainerClient.getBlobClient(sourceBlobName);
        if (!sourceBlobClient.exists()) {
            throw new IOException("Source file not found: " + sourceBlobName);
        }

        // Get destination blob client
        BlobClient destinationBlobClient = blobcontainerClient.getBlobClient(destinationBlobName);

        
        logger.info("Moving blob from {} to {}", sourceBlobName, destinationBlobName);
        
        try {
            // Copy the blob to the new location
            String sourceUrl = sourceBlobClient.getBlobUrl();
            destinationBlobClient.copyFromUrl(sourceUrl);

            // Verify the destination blob exists
            if (!destinationBlobClient.exists()) {
                throw new IOException("Failed to copy blob to destination");
            }

            // Delete the source blob
            sourceBlobClient.delete();

        
            logger.info("Successfully moved blob from {} to {}", sourceBlobName, destinationBlobName);
        

        } catch (Exception e) {
            throw new IOException("Failed to move file from folder " + sourceFolder + " to " + destinationFolder, e);
        }
    }

 
}
