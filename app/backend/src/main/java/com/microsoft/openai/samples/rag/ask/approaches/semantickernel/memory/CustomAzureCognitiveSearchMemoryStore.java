package com.microsoft.openai.samples.rag.ask.approaches.semantickernel.memory;

import com.azure.core.credential.TokenCredential;
import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchQueryVector;
import com.microsoft.semantickernel.ai.embeddings.Embedding;
import com.microsoft.semantickernel.connectors.memory.azurecognitivesearch.AzureCognitiveSearchMemoryRecord;
import com.microsoft.semantickernel.connectors.memory.azurecognitivesearch.AzureCognitiveSearchMemoryStore;
import com.microsoft.semantickernel.memory.MemoryRecord;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CustomAzureCognitiveSearchMemoryStore extends AzureCognitiveSearchMemoryStore {

    private SearchAsyncClient searchClient;
    private String embeddingFieldMapping = "Embedding";

    private Function<SearchDocument,MemoryRecord> memoryRecordMapper ;
    /**
     * Create a new instance of custom  memory storage using Azure Cognitive Search.
     *
     * @param endpoint Azure Cognitive Search URI, e.g. "https://contoso.search.windows.net"
     * @param credentials Azure service credentials
     * @param searchClient Another instance of cognitive search client. Unfortunately this is a hack as
     *                     current getSearchClient is private in parent class.
     */
    public CustomAzureCognitiveSearchMemoryStore(
            @Nonnull String endpoint, @Nonnull TokenCredential credentials, @Nonnull SearchAsyncClient searchClient, String embeddingFieldMapping) {
        super(endpoint, credentials);
        this.searchClient = searchClient;
        if(embeddingFieldMapping != null && !embeddingFieldMapping.isEmpty())
            this.embeddingFieldMapping = embeddingFieldMapping ;

    }

    public CustomAzureCognitiveSearchMemoryStore(
            @Nonnull String endpoint, @Nonnull TokenCredential credentials, @Nonnull SearchAsyncClient searchClient, String embeddingFieldMapping, Function<SearchDocument,MemoryRecord> memoryRecordMapper) {
        this(endpoint,credentials,searchClient,embeddingFieldMapping);
        this.memoryRecordMapper = memoryRecordMapper ;
    }

    public Mono<Collection<Tuple2<MemoryRecord, Float>>> getNearestMatchesAsync(
            @Nonnull String collectionName,
            @Nonnull Embedding embedding,
            int limit,
            float minRelevanceScore,
            boolean withEmbedding) {

        SearchQueryVector searchVector =
                new SearchQueryVector()
                        .setKNearestNeighborsCount(limit)
                        .setFields(embeddingFieldMapping)
                        .setValue(embedding.getVector());

        SearchOptions searchOptions = new SearchOptions().setVectors(searchVector);

        return searchClient.search(null, searchOptions)
                .filter(result -> (double) minRelevanceScore <= result.getScore())
                .map(
                        result -> {
                            MemoryRecord memoryRecord;
                            //Use default SK mapper if no custom mapper is provided
                            if(this.memoryRecordMapper == null) {
                                    memoryRecord = result.getDocument(AzureCognitiveSearchMemoryRecord.class)
                                              .toMemoryRecord(withEmbedding);
                                  } else {
                                    memoryRecord = this.memoryRecordMapper.apply(result.getDocument(SearchDocument.class));
                                    }

                            float score = (float) result.getScore();
                            return Tuples.of(memoryRecord, score);
                        })
                .collect(Collectors.toList());
    }

}
