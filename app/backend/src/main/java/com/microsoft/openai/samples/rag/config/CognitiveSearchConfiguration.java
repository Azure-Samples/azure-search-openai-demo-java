package com.microsoft.openai.samples.rag.config;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CognitiveSearchConfiguration {

    @Value("${cognitive.search.service}") String searchServiceName ;
    @Value("${cognitive.search.index}") String indexName;

    @Autowired
    TokenCredential tokenCredential;

    @Bean
    @ConditionalOnProperty(name = "openai.tracing.enabled", havingValue = "true")
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
    @ConditionalOnProperty(name = "openai.tracing.enabled", havingValue = "false")
    public SearchClient searchDefaultClient() {
        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .indexName(indexName)
                .buildClient();
    }


}
