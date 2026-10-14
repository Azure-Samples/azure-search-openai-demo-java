// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureAISearchEmbeddingStoreConfiguration {

    @Bean
    public AzureAiSearchEmbeddingStore azureAiSearchEmbeddingStore(@Value("${azureai.search.service}") String searchservice,
                                                                   @Value("${azureai.search.index}") String index,
                                                                   @Value("${openai.embedding.dimension}") int dimensions,
                                                                   TokenCredential tokenCredential){
        String endpoint = "https://%s.search.windows.net".formatted(searchservice);
        return  AzureAiSearchEmbeddingStore.builder()
                .endpoint(endpoint)
                .indexName(index)
                .dimensions(dimensions)
                .tokenCredential(tokenCredential)
                .createOrUpdateIndex(false)
                .build();
    }

}
