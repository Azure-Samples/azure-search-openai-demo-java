package com.microsoft.openai.samples.indexer.langchain4j.embedding;

import com.azure.core.credential.TokenCredential;
import com.microsoft.openai.samples.indexer.langchain4j.ConfigUtils;
import com.microsoft.openai.samples.indexer.langchain4j.IndexingConfigException;
import com.microsoft.openai.samples.indexer.langchain4j.providers.EmbeddingStoreProvider;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;

import java.util.function.Function;

/**
 * Factory class to create instances of EmbeddingStoreProvider based on EmbeddingStoreConfig.
 * Currently supports Azure AI Search embedding store.
 */
public class EmbeddingStoreProviderFactory {
    private final Function<String, Object> DIResolver;

    public EmbeddingStoreProviderFactory(Function<String, Object> diResolver) {
        DIResolver = diResolver;
    }

   public EmbeddingStoreProvider<TextSegment> create(EmbeddingStoreConfig config) {
        if (config == null || config.type() == null) {
            throw new IndexingConfigException("EmbeddingModelConfig and its type must not be null");
        }

        switch (config.type()) {
            case "langchain4j-azure-ai-search-store":
                return buildLangchain4JAzureAISearch(config);
            default:
                throw new IllegalArgumentException("Unsupported embedding store type: " + config.type());
        }

    }

    private EmbeddingStoreProvider<TextSegment> buildLangchain4JAzureAISearch(EmbeddingStoreConfig config) {
        var azureAISearchName = ConfigUtils.getString("service-name", config.params());
        var indexName = ConfigUtils.getString("index-name", config.params());
        var createOrUpdate = ConfigUtils.parseBooleanOrDefault(config.params(), "create-or-update", false);
        var identityRef = ConfigUtils.getString("identity-ref", config.params());
        var tokenCredential = (TokenCredential) DIResolver.apply(identityRef);
        var dimensions = ConfigUtils.parseIntOrDefault(config.params(),"dimensions",3072 );

        String endpoint = "https://%s.search.windows.net".formatted(azureAISearchName);
        return ctx -> AzureAiSearchEmbeddingStore.builder()
                .endpoint(endpoint)
                .indexName(indexName)
                .dimensions(dimensions)
                .tokenCredential(tokenCredential)
                .createOrUpdateIndex(createOrUpdate)
                .build();

        }


}
