// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.indexer.service.config;

import com.azure.core.credential.TokenCredential;
import com.microsoft.openai.samples.indexer.parser.DocumentIntelligencePDFParser;
import com.microsoft.openai.samples.indexer.storage.BlobManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentIntelligencePDFParserConfiguration {

    @Bean
    public DocumentIntelligencePDFParser documentIntelligencePDFParser ( @Value("${formrecognizer.name}") String formRecognizerName,
                                     TokenCredential tokenCredential){
    return  new DocumentIntelligencePDFParser(formRecognizerName,tokenCredential,true);
    }
}
