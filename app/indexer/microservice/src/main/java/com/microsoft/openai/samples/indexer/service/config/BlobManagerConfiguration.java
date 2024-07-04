// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
@Configuration
public class BlobManagerConfiguration {

    @Bean
    public BlobManager blobManager ( @Value("${storage-account.service}") String storageAccountServiceName,
                                     @Value("${blob.container.name}") String containerName,
                                     TokenCredential tokenCredential){
    return new BlobManager(storageAccountServiceName, containerName, tokenCredential, false);
    }
}
