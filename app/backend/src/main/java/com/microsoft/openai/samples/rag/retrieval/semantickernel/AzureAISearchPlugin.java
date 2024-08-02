// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval.semantickernel;

import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.AzureAISearchRetriever;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * This class is Semantic Kernel plugin that provides a kernel semantic function to search information relevant to answering a given query.
 * It uses Azure AI Search to retrieve information from the search index. The implementation simply delegates to the common AzureAISearchRetriever class which is also used
 * by the RAG approach based on java plain open ai client sdk.
 */
public class AzureAISearchPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAISearchPlugin.class);
    private final AzureAISearchProxy azureAISearchProxy;
    private final OpenAIProxy openAIProxy;
    private final RAGOptions options;

    public AzureAISearchPlugin(
            AzureAISearchProxy azureAISearchProxy,
            OpenAIProxy openAIProxy,
            RAGOptions options) {
        this.azureAISearchProxy = azureAISearchProxy;
        this.options = options;
        this.openAIProxy = openAIProxy;
    }


    @DefineKernelFunction(
            name = "SearchFromQuestion",
            description = "Search information relevant to answering a given query",
            returnType = "string")
    public Mono<String> searchFromQuestion(
            @KernelFunctionParameter(
                    description = "the query to answer",
                    name = "query")
            String query) {

        AzureAISearchRetriever retriever =
                new AzureAISearchRetriever(this.azureAISearchProxy, this.openAIProxy);
        List<ContentSource> sources = retriever.retrieveFromQuestion(query, this.options);

        LOGGER.info(
                "Total {} sources found in Azure AI search index for keyword search query[{}]",
                sources.size(),
                query);

        return Mono.just(buildSources(sources));
    }

    @DefineKernelFunction(
            name = "SearchFromConversation",
            description = "Search information relevant to a conversation",
            returnType = "string")
    public Mono<String> searchFromConversation(
            @KernelFunctionParameter(
                    description = "the conversation to search the information from",
                    name = "conversation"
            ) String conversation) {
        // Parse conversation
        List<ChatGPTMessage> chatMessages = ChatGPTUtils.parseChatML(conversation).stream().map(message ->
                new ChatGPTMessage(ChatGPTMessage.ChatRole.fromString(message.getAuthorRole().toString()), message.getContent())
        ).toList();

        AzureAISearchRetriever retriever =
                new AzureAISearchRetriever(this.azureAISearchProxy, this.openAIProxy);
        List<ContentSource> sources = retriever.retrieveFromConversation(new ChatGPTConversation(chatMessages), this.options);

        LOGGER.info(
                "Total {} sources found in Azure AI search",
                sources.size());

        return Mono.just(buildSources(sources));
    }

    private String buildSources(List<ContentSource> sources) {
        StringBuilder sourcesStringBuilder = new StringBuilder();

        sources.iterator()
                .forEachRemaining(
                        source ->
                                sourcesStringBuilder
                                        .append(source.getSourceName())
                                        .append(": ")
                                        .append(source.getSourceContent().replace("\n", ""))
                                        .append("\n"));
        return sourcesStringBuilder.toString();
    }
}
