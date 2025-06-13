package com.microsoft.openai.samples.indexer.cli.langchain4j;

import com.microsoft.openai.samples.indexer.storage.BlobManager;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UploadCommand {

    private static final Logger logger = LoggerFactory.getLogger(UploadCommand.class);
    private final BlobManager blobManager;
    private final AzureAiSearchEmbeddingStore azureAiSearchEmbeddingStore;
    private final int dimensions;
    private final HttpClient httpClient;
    private final String indexerApiUrl;

    public UploadCommand(BlobManager blobManager, AzureAiSearchEmbeddingStore azureAiSearchEmbeddingStore, int dimensions, String indexerApiUrl) {
        this.blobManager = blobManager;
        this.azureAiSearchEmbeddingStore = azureAiSearchEmbeddingStore;
        this.dimensions = dimensions;
        this.httpClient = HttpClient.newBuilder().build();
        this.indexerApiUrl = indexerApiUrl;
    }

    public void run(Path path,String category){

        azureAiSearchEmbeddingStore.createOrUpdateIndex(dimensions);

        if(Files.isDirectory(path))
            uploadDirectory(path, category);
        else
            uploadFile(path);    }

    private void uploadDirectory( Path directory, String category) {
        logger.debug("Uploading directory {}", directory);
        try {
            Files.newDirectoryStream(directory).forEach(path -> {
                uploadFile(path);
            });
            logger.debug("All files in directory {} have been uploaded", directory.toRealPath().toString());        } catch (Exception e) {
            throw new RuntimeException("Error processing folder ",e);
        }
    }

    private void uploadFile(Path path) {
        try {
            String absoluteFilePath = path.toRealPath().toString();
            //Files uploaded through CLI go to staging folder
            blobManager.uploadFileToFolder("staging/",path.toFile());
            logger.info("file {} uploaded to staging", absoluteFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Error processing file ",e);
        }
    }

    private void uploadAndTriggerIndexing(Path path, String category) {
        try {
            String absoluteFilePath = path.toRealPath().toString();
           

            blobManager.uploadFileToFolder(null, path.toFile());
            logger.info("file {} uploaded to storage", absoluteFilePath);
                      

            //not logged user uploading goes to default folder
            String fileName = "default/"+path.getFileName().toString();
            
            logger.info("Triggering indexing for file {}", fileName);
           
            // call the indexer rest api to asynchronously trigger indexing
            CompletableFuture.runAsync(() -> {
            try {
                // Create JSON payload matching IndexingRequest structure
                String requestBody = String.format(
                    "{\"fileOrUrlpath\":\"%s\",\"metadata\":[{\"key\":\"oid\",\"value\":\"default\",\"isFilterable\":true}]}", 
                    fileName
                );
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(indexerApiUrl + "/api/index/add"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                logger.debug("Indexing triggered successfully for file: {}", fileName);
                            } else {
                                logger.warn("Failed to trigger indexing for file: {}, status: {}", fileName, response.statusCode());
                            }
                        })
                        .exceptionally(throwable -> {
                            logger.error("Error triggering indexing for file: {}", fileName, throwable);
                            return null;
                        });
                        
            } catch (Exception e) {
                logger.error("Error creating indexing request for file: {}", fileName, e);
            }
        });
            
        } catch (Exception e) {
            throw new RuntimeException("Error processing file %s".formatted(path), e);
        }
    }    

}
