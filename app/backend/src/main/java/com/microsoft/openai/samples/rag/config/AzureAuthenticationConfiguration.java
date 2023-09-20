package com.microsoft.openai.samples.rag.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AzureAuthenticationConfiguration {

    @Profile("dev")
    @Bean
    public TokenCredential localTokenCredential() {
        return new AzureCliCredentialBuilder().build();
    }

    @Bean
    @Profile("default")
    public TokenCredential managedIdentityTokenCredential() {
        return new ManagedIdentityCredentialBuilder().build();
    }

}
