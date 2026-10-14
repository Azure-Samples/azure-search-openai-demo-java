package com.microsoft.openai.samples.rag.config;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableCosmosRepositories(basePackages = "com.microsoft.openai.samples.rag.history")
@ConditionalOnProperty(name = "app.showChatHistoryCosmos", havingValue = "true")
public class CosmosConfiguration extends AbstractCosmosConfiguration {

    @Value("${app.cosmosdb.accountName}")
    private String accountName;

    @Value("${app.cosmosdb.databaseName}")
    private String databaseName;

    private final TokenCredential tokenCredential;

    public CosmosConfiguration(TokenCredential tokenCredential) {
        this.tokenCredential = tokenCredential;
    }

    @Bean
    public CosmosClientBuilder getCosmosClientBuilder() {

        String endpoint = String.format("https://%s.documents.azure.com:443/", accountName);
        return new CosmosClientBuilder()
            .endpoint(endpoint)
            .credential(tokenCredential);

    }

    @Override
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
            .enableQueryMetrics(true)
            .build();
    }

    @Override
    protected String getDatabaseName() {
        return databaseName != null ? databaseName : "chat-history-v2";
    }
}
