package com.microsoft.openai.samples.indexer.service.controller;

import com.microsoft.openai.samples.indexer.service.langchain4j.Langchain4JIndexerService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(IndexController.class);
    private final Langchain4JIndexerService indexerService;
    private final AzureAiSearchEmbeddingStore azureAiSearchEmbeddingStore;

    public IndexController(Langchain4JIndexerService indexerService, AzureAiSearchEmbeddingStore azureAiSearchEmbeddingStore) {
        this.indexerService = indexerService;
        this.azureAiSearchEmbeddingStore = azureAiSearchEmbeddingStore;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addFile(@RequestBody IndexingRequest request) {

        LOGGER.info("Received request to index file or URL: {}", request.fileOrUrlpath());

        Metadata indexingMetadata = new Metadata();
        if (request.metadata() != null) {
            request.metadata().forEach(m -> indexingMetadata.put(m.key(), m.value()));
        }

        // Use Langchain4JIndexerService to index the file or URL
        indexerService.index(request.fileOrUrlpath(), indexingMetadata);


        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deletefile(@RequestBody IndexingRequest request) {
        LOGGER.info("Received request to delete index for: {}", request.fileOrUrlpath());


        IsEqualTo filter = new IsEqualTo("file_path", request.fileOrUrlpath());
        azureAiSearchEmbeddingStore.removeAll(filter);
        LOGGER.info("Index successfully removed for {}", request.fileOrUrlpath());
        return ResponseEntity.ok().build();
    }

}

