// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.core.credential.TokenCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentIntelligenceConfiguration {

    @Bean
    public DocumentAnalysisClient documentAnalysisClient (
            @Value("${document-intelligence.service}") String serviceName,
            TokenCredential tokenCredential){

        return new DocumentAnalysisClientBuilder()
                .endpoint("https://%s.cognitiveservices.azure.com/".formatted(serviceName))
                .credential(tokenCredential)
                .buildClient();
    }
}
