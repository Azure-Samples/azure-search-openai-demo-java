// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.openai.samples.indexer.embeddings.AzureOpenAIEmbeddingService;
import com.microsoft.openai.samples.indexer.index.AzureSearchClientFactory;
import com.microsoft.openai.samples.indexer.index.SearchIndexManager;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchIndexManagerConfiguration {

    @Bean
    public AzureOpenAIEmbeddingService azureOpenAIEmbeddingService(@Value("${openai.embedding.deployment}") String openaiEmbdeployment,
                                                                   @Value("${openai.service}") String openaiServiceName,
                                                                   TokenCredential tokenCredential) {
        return new AzureOpenAIEmbeddingService(openaiServiceName, openaiEmbdeployment, tokenCredential, false);
    }
    @Bean
    public AzureSearchClientFactory azureSearchClientFactory(@Value("${cognitive.search.service}") String searchservice,
                                                             @Value("${cognitive.search.index}") String index,
                                                             TokenCredential tokenCredential) {
        return new AzureSearchClientFactory(searchservice, tokenCredential, index, false);
    }

    @Bean
    public SearchIndexManager searchIndexManager(AzureSearchClientFactory azureSearchClientFactory,
                                                 AzureOpenAIEmbeddingService azureOpenAIEmbeddingService,
                                                 @Value("${cognitive.search.analizername}") String searchAnalyzerName) {

        return new SearchIndexManager(azureSearchClientFactory,
                searchAnalyzerName,
                azureOpenAIEmbeddingService);
    }

}
