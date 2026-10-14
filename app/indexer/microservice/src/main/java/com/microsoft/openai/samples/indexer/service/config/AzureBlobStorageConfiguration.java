// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.core.credential.TokenCredential;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobStorageConfiguration {
   @Value("${storage-account.service}")
   String storageAccountServiceName;
   @Value("${blob.container.name}")
   String containerName;



   TokenCredential tokenCredential;

    public AzureBlobStorageConfiguration(TokenCredential tokenCredential){
        this.tokenCredential = tokenCredential;

    }

    @Bean
    public BlobManager blobManager (){
            return new BlobManager(storageAccountServiceName, containerName, tokenCredential);

    }
}
