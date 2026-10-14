// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.config;


import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClient;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Langchain4JConfiguration {

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    @Value("${openai.embedding.deployment}")
    private String azureOpenAIDeploymentName;

    @Value("${openai.embedding.dimension}")
    private int dimensions;

    @Value("${openai.requests.temperature}")
    private double temperature;

    @Value("${openai.requests.maxTokens}")
    private Integer maxTokens;

    @Bean
    public ChatModel chatLanguageModel(OpenAIClient azureOpenAICLient, OpenAIAsyncClient asyncOpenAIClient) {

        return AzureOpenAiChatModel.builder()
                .openAIClient(azureOpenAICLient)
                .deploymentName(gptChatDeploymentModelId)
                .logRequestsAndResponses(true)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    @Bean
    public StreamingChatModel chatLanguageModelAsync(OpenAIClient openAIClient,OpenAIAsyncClient asyncOpenAIClient) {

        return AzureOpenAiStreamingChatModel.builder()
                //.openAIAsyncClient(asyncOpenAIClient)
                .openAIClient(openAIClient)
                .deploymentName(gptChatDeploymentModelId)
                .logRequestsAndResponses(true)
                .build();
    }


    @Bean
    public EmbeddingModel embeddingModel(OpenAIClient openAIClient) {
        return AzureOpenAiEmbeddingModel.builder()
                .openAIClient(openAIClient)
                .deploymentName(azureOpenAIDeploymentName)
                .dimensions(dimensions)
                .build();
    }




}
