// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.EnvironmentCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.openai.samples.indexer.service.BlobMessageConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class AzureAuthenticationConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AzureAuthenticationConfiguration.class);


    @Value("${azure.identity.client-id}")
    String clientId;

    @Profile("dev")
    @Bean
    @Primary
    public TokenCredential localTokenCredential() {
        logger.info("Dev Profile activated using AzureCliCredentialBuilder");
        return new AzureCliCredentialBuilder().build();
    }

    @Profile("docker")
    @Bean
    @Primary
    public TokenCredential servicePrincipalTokenCredential() {
        return new EnvironmentCredentialBuilder().build();
    }
    @Bean
    @Profile("default")
    @Primary
    public TokenCredential managedIdentityTokenCredential() {
        logger.info("Using identity with client id: {}", this.clientId);
        if (this.clientId.equals("system-managed-identity"))
            return new ManagedIdentityCredentialBuilder().build();
        else
            return new ManagedIdentityCredentialBuilder().clientId(this.clientId).build();

    }
}
