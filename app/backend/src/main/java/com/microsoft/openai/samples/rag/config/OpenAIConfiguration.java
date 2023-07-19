package com.microsoft.openai.samples.rag.config;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class OpenAIConfiguration {

    @Value("${openai.service}") String openAIServiceName;

    @Autowired
    TokenCredential tokenCredential;

    @Bean
    @ConditionalOnProperty(name = "openai.tracing.enabled", havingValue = "true")
    public OpenAIClient tracingEnabledClient() {
        String endpoint = "https://%s.openai.azure.com".formatted(openAIServiceName);

        var httpLogOptions = new HttpLogOptions();
        httpLogOptions.setPrettyPrintBody(true);
        httpLogOptions.setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS);

        return new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .httpLogOptions(httpLogOptions)
                .buildClient();

    }


    @Bean
    @ConditionalOnProperty(name = "openai.tracing.enabled", havingValue = "false")
    public OpenAIClient defaultClient() {
        String endpoint = "https://%s.openai.azure.com".formatted(openAIServiceName);
        return new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(tokenCredential)
                .buildClient();
    }


}
