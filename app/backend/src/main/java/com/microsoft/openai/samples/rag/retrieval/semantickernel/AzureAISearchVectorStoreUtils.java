package com.microsoft.openai.samples.rag.retrieval.semantickernel;

import com.azure.search.documents.models.QueryCaption;
import com.azure.search.documents.models.QueryCaptionType;
import com.azure.search.documents.models.QueryType;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SemanticSearchOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RetrievalMode;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.data.azureaisearch.AzureAISearchVectorStoreRecordCollection;
import com.microsoft.semantickernel.data.vectorsearch.VectorSearchResult;
import com.microsoft.semantickernel.data.vectorsearch.VectorSearchResults;
import com.microsoft.semantickernel.data.vectorstorage.annotations.VectorStoreRecordData;
import com.microsoft.semantickernel.data.vectorstorage.annotations.VectorStoreRecordKey;
import com.microsoft.semantickernel.data.vectorstorage.annotations.VectorStoreRecordVector;
import com.microsoft.semantickernel.data.vectorstorage.definition.DistanceFunction;
import com.microsoft.semantickernel.data.vectorstorage.options.VectorSearchOptions;
import com.microsoft.semantickernel.services.ServiceNotFoundException;
import com.microsoft.semantickernel.services.textembedding.EmbeddingGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureAISearchVectorStoreUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAISearchVectorStoreUtils.class);

    private static final String EMBEDDING_FIELD_NAME = "embedding";

    public static class DocumentRecord {
        @VectorStoreRecordKey
        private final String id;
        @VectorStoreRecordData
        private final String content;
        @VectorStoreRecordVector(dimensions = 1536, distanceFunction = DistanceFunction.COSINE_DISTANCE)
        private final List<Float> embedding;
        @VectorStoreRecordData
        private final String category;
        @JsonProperty("sourcepage")
        @VectorStoreRecordData
        private final String sourcePage;
        @JsonProperty("sourcefile")
        @VectorStoreRecordData
        private final String sourceFile;

        public DocumentRecord(
                @JsonProperty("id") String id,
                @JsonProperty("content") String content,
                @JsonProperty("embedding") List<Float> embedding,
                @JsonProperty("sourcepage") String sourcePage,
                @JsonProperty("sourcefile") String sourceFile,
                @JsonProperty("category") String category) {
            this.id = id;
            this.content = content;
            this.embedding = embedding;
            this.sourcePage = sourcePage;
            this.sourceFile = sourceFile;
            this.category = category;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public List<Float> getEmbedding() {
            return embedding;
        }

        public String getCategory() {
            return category;
        }

        public String getSourcePage() {
            return sourcePage;
        }

        public String getSourceFile() {
            return sourceFile;
        }
    }


    public static List<DocumentRecord> searchAsync(String searchQuery,
                                                   Kernel kernel,
                                                   AzureAISearchVectorStoreRecordCollection<DocumentRecord> recordCollection,
                                                   RAGOptions ragOptions) {
        // Create VectorSearch options
        VectorSearchOptions vectorSearchOptions = VectorSearchOptions.builder()
                .withTop(ragOptions.getTop())
                .withVectorFieldName(EMBEDDING_FIELD_NAME)
                .build();

        // Vector to search
        List<Float> questionVector = null;

        // Additional AzureAISearch options
        SearchOptions searchOptions = getAdditionalSearchOptions(ragOptions);

        // If the retrieval mode is set to vectors or hybrid, convert the user's query text to an
        // embeddings vector. The embeddings vector is passed as search options to Azure AI Search index
        if (ragOptions.getRetrievalMode() == RetrievalMode.vectors
                || ragOptions.getRetrievalMode() == RetrievalMode.hybrid) {
            LOGGER.info(
                    "Retrieval mode is set to {}. Retrieving vectors for question [{}]",
                    ragOptions.getRetrievalMode(),
                    searchQuery);

            try {
                // Get the embedding service from the kernel
                EmbeddingGenerationService<String> embeddingService = (EmbeddingGenerationService<String>) kernel.getService(EmbeddingGenerationService.class);
                // Generate the embeddings
                questionVector = embeddingService.generateEmbeddingAsync(searchQuery).block().getVector();
            } catch (ServiceNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // Search the vector store for the relevant documents with the generated embeddings
        VectorSearchResults<DocumentRecord> memoryResult = recordCollection.hybridSearchAsync(searchQuery, questionVector, vectorSearchOptions, searchOptions)
                .block();

        // Remove the score from the result
        return memoryResult.getResults().stream().map(VectorSearchResult::getRecord).collect(Collectors.toList());
    }


    public static List<ContentSource> buildSources(List<DocumentRecord> memoryResult) {
        return memoryResult
                .stream()
                .map(result -> {
                    return new ContentSource(
                            result.getSourcePage(),
                            result.getContent()
                    );
                })
                .collect(Collectors.toList());
    }


    public static String buildSourcesText(List<DocumentRecord> memoryResult) {
        StringBuilder sourcesContentBuffer = new StringBuilder();
        memoryResult.stream().forEach(memory -> {
            sourcesContentBuffer.append(memory.getSourceFile())
                    .append(": ")
                    .append(memory.getContent().replace("\n", ""))
                    .append("\n");
        });
        return sourcesContentBuffer.toString();
    }


    private static SearchOptions getAdditionalSearchOptions(RAGOptions options) {
        SearchOptions searchOptions = new SearchOptions();

        Optional.ofNullable(options.getTop())
                .ifPresentOrElse(searchOptions::setTop, () -> searchOptions.setTop(3));
        Optional.ofNullable(options.getExcludeCategory())
                .ifPresentOrElse(
                        value ->
                                searchOptions.setFilter(
                                        "category ne '%s'".formatted(value.replace("'", "''"))),
                        () -> searchOptions.setFilter(null));

        Optional.ofNullable(options.isSemanticRanker())
                .ifPresent(
                        isSemanticRanker -> {
                            if (isSemanticRanker) {
                                searchOptions.setQueryType(QueryType.SEMANTIC);
                                searchOptions.setSemanticSearchOptions(
                                        new SemanticSearchOptions()
                                                .setSemanticConfigurationName("default")
                                                .setQueryCaption(
                                                        new QueryCaption(QueryCaptionType.EXTRACTIVE)
                                                                .setHighlightEnabled(false)
                                                )
                                );
                            }
                        });
        return searchOptions;
    }
}
