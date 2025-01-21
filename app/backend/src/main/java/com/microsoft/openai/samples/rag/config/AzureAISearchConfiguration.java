// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexAsyncClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureAISearchConfiguration {

    @Value("${cognitive.search.service}")
    String searchServiceName;

    @Value("${cognitive.search.index}")
    String indexName;

    final TokenCredential tokenCredential;

    public AzureAISearchConfiguration(TokenCredential tokenCredential) {
        this.tokenCredential = tokenCredential;
    }

    @Bean
    @ConditionalOnProperty(name = "cognitive.tracing.enabled", havingValue = "true")
    public SearchClient searchTracingEnabledClient() {
        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);

        var httpLogOptions = new HttpLogOptions();
        httpLogOptions.setPrettyPrintBody(true);
        httpLogOptions.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);

        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .indexName(indexName)
                .httpLogOptions(httpLogOptions)
                .buildClient();
    }

    @Bean
    @ConditionalOnProperty(name = "cognitive.tracing.enabled", havingValue = "false")
    public SearchClient searchDefaultClient() {
        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .indexName(indexName)
                .buildClient();
    }

    @Bean
    @ConditionalOnProperty(name = "cognitive.tracing.enabled", havingValue = "true")
    public SearchAsyncClient asyncSearchTracingEnabledClient() {
        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);

        var httpLogOptions = new HttpLogOptions();
        httpLogOptions.setPrettyPrintBody(true);
        httpLogOptions.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);

        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .indexName(indexName)
                .httpLogOptions(httpLogOptions)
                .buildAsyncClient();
    }

    @Bean
    @ConditionalOnProperty(name = "cognitive.tracing.enabled", havingValue = "false")
    public SearchAsyncClient asyncSearchDefaultClient() {
        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .indexName(indexName)
                .buildAsyncClient();
    }

    @Bean
    @ConditionalOnProperty(name = "cognitive.tracing.enabled", havingValue = "true")
    public SearchIndexAsyncClient asyncSearchIndexDefaultClient() {
        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);

        return new SearchIndexClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .buildAsyncClient();
    }
}
