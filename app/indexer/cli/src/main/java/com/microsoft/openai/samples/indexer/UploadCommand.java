package com.microsoft.openai.samples.indexer;

import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class UploadCommand {

    private static final Logger logger = LoggerFactory.getLogger(UploadCommand.class);
    private final SearchIndexManager searchIndexManager;
    private final BlobManager blobManager;

    public UploadCommand(SearchIndexManager searchIndexManager, BlobManager blobManager) {
        this.searchIndexManager = searchIndexManager;
        this.blobManager = blobManager;
    }

    public void run(Path path,String category){

        searchIndexManager.createIndex();

        if(Files.isDirectory(path))
            uploadDirectory(path);
        else
            uploadFile(path);
    }

    private void uploadDirectory( Path directory) {
        logger.debug("Uploading directory {}", directory);
        try {
            Files.newDirectoryStream(directory).forEach(path -> {
                uploadFile(path);
            });
            logger.debug("All files in directory {} have been uploaded", directory.toRealPath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Error processing folder ",e);
        }
    }

    private void uploadFile(Path path) {
        try {
            String absoluteFilePath = path.toRealPath().toString();
            blobManager.uploadBlob(path.toFile());
            logger.debug("file {} uploaded", absoluteFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Error processing file ",e);
        }
    }
}
