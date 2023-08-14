package com.microsoft.openai.samples.rag.test.config;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.proxy.BlobStorageProxy;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class ProxyMockConfiguration {


    @Bean
    @Primary
    public CognitiveSearchProxy mockedCognitiveSearchProxy() { return Mockito.mock(CognitiveSearchProxy.class); }

    @Bean
    @Primary
    public OpenAIProxy mockedOpenAISearchProxy() {
        return Mockito.mock(OpenAIProxy.class);
    }

    @Bean
    @Primary
    public BlobStorageProxy mockedBlobStorageProxy() {
        return Mockito.mock(BlobStorageProxy.class);
    }

    @Bean
    @Primary
    public OpenAIAsyncClient mockedOpenAIAsynchClient() { return Mockito.mock(OpenAIAsyncClient.class);
    }

}
