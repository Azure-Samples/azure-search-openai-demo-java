// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
public class AzureAuthenticationConfiguration {

    @Value("${azure.identity.client-id}")
    String clientId;

    @Profile("dev")
    @Bean
    @Primary
    public TokenCredential localTokenCredential() {
        return new AzureCliCredentialBuilder().build();
    }

    @Bean
    @Profile("default")
    @Primary
    public TokenCredential managedIdentityTokenCredential() {
        if (this.clientId.equals("system-managed-identity"))
            return new ManagedIdentityCredentialBuilder().build();
        else
            return new ManagedIdentityCredentialBuilder().clientId(this.clientId).build();

    }
}
