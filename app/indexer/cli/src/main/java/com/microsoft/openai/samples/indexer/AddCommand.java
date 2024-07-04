package com.microsoft.openai.samples.indexer;

import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class AddCommand {

    private static final Logger logger = LoggerFactory.getLogger(AddCommand.class);
    private final SearchIndexManager searchIndexManager;
    private final BlobManager blobManager;
    private final DocumentProcessor documentProcessor;

    public AddCommand(SearchIndexManager searchIndexManager, BlobManager blobManager, DocumentProcessor documentProcessor) {
        this.searchIndexManager = searchIndexManager;
        this.blobManager = blobManager;
        this.documentProcessor = documentProcessor;
    }

    public void run(Path path,String category){

        searchIndexManager.createIndex();

        if(Files.isDirectory(path))
            processDirectory(path,category);
        else
            processFile(path,category);
    }

    private void processDirectory(Path directory, String category) {
        logger.debug("Processing directory {}", directory);
        try {
            Files.newDirectoryStream(directory).forEach(path -> {
                processFile(path,category);
            });
            logger.debug("All files in directory {} processed", directory.toRealPath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Error processing folder ",e);
        }
    }

    private void processFile(Path path,String category) {
        try {
            String absoluteFilePath = path.toRealPath().toString();
            documentProcessor.indexDocumentfromFile(absoluteFilePath,category);
            logger.debug("file {} indexed", absoluteFilePath);
            blobManager.uploadBlob(path.toFile());
            logger.debug("file {} uploaded", absoluteFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Error processing file ",e);
        }
    }
}
