// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use Java Semantic Kernel framework with semantic and native functions chaining. It uses an
 * imperative style for AI orchestration through sequentially call semantic kernel functions.
 * InformationFinder.SearchFromQuestion native function and RAG.AnswerQuestion semantic function are called
 * sequentially. Several Azure AI Search retrieval options are available: Text, Vector, Hybrid.
 */
@Component
public class JavaSemanticKernelChainsApproach implements RAGApproach<String, RAGResponse> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JavaSemanticKernelChainsApproach.class);
    private static final String PLAN_PROMPT =
            """
                    Take the input as a question and answer it finding any information needed
                    """;
    private final AzureAISearchProxy azureAISearchProxy;

    private final OpenAIProxy openAIProxy;

    private final OpenAIAsyncClient openAIAsyncClient;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public JavaSemanticKernelChainsApproach(
            AzureAISearchProxy azureAISearchProxy,
            OpenAIAsyncClient openAIAsyncClient,
            OpenAIProxy openAIProxy) {
        this.azureAISearchProxy = azureAISearchProxy;
        this.openAIAsyncClient = openAIAsyncClient;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {

        // Build semantic kernel context
        Kernel semanticKernel = buildSemanticKernel(options);

        // STEP 1: Retrieve relevant documents using user question. It reuses the
        // AzureAISearchRetriever appraoch through the AzureAISearchPlugin native function.
        FunctionResult<String> searchContext = semanticKernel
                .getPlugin("InformationFinder")
                .get("SearchFromQuestion")
                .invokeAsync(semanticKernel)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withInput(question)
                                .build()
                )
                .withResultType(String.class)
                .block();

        var sources = formSourcesList(searchContext.getResult());

        // STEP 2: Build a SK context with the sources retrieved from the memory store and the user
        // question.
        var answerVariables =
                KernelFunctionArguments.builder()
                        .withVariable("sources", searchContext.getResult())
                        .withVariable("input", question)
                        .build();

        /**
         * STEP 3: Get a reference of the semantic function [AnswerQuestion] of the [RAG] plugin
         * (a.k.a. skill) from the SK skills registry and provide it with the pre-built context.
         * Triggering Open AI to get an answerVariables.
         */
        FunctionResult<String> answerExecutionContext = semanticKernel
                .invokeAsync("RAG", "AnswerQuestion")
                .withArguments(answerVariables)
                .withResultType(String.class)
                .block();

        return new RAGResponse.Builder()
                .prompt("Prompt is managed by Semantic Kernel")
                .answer(answerExecutionContext.getResult())
                .sources(sources)
                .sourcesAsText(searchContext.getResult())
                .question(question)
                .build();
    }

    @Override
    public void runStreaming(
            String questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    private List<ContentSource> formSourcesList(String result) {
        if (result == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(result.split("\n"))
                .map(
                        source -> {
                            String[] split = source.split(":", 2);
                            if (split.length >= 2) {
                                var sourceName = split[0].trim();
                                var sourceContent = split[1].trim();
                                return new ContentSource(sourceName, sourceContent);
                            } else {
                                return null;
                            }
                        })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Build semantic kernel context with AnswerQuestion semantic function and
     * InformationFinder.SearchFromQuestion native function. AnswerQuestion is imported from
     * src/main/resources/semantickernel/Plugins. InformationFinder.SearchFromQuestion is implemented in a
     * traditional Java class method: AzureAISearchPlugin.searchFromConversation
     *
     * @param options
     * @return
     */
    private Kernel buildSemanticKernel(RAGOptions options) {
        return Kernel.builder()
                .withAIService(
                        ChatCompletionService.class,
                        OpenAIChatCompletion.builder()
                                .withModelId(gptChatDeploymentModelId)
                                .withOpenAIAsyncClient(this.openAIAsyncClient)
                                .build()
                )
                .withPlugin(
                        KernelPluginFactory.createFromObject(
                                new AzureAISearchPlugin(this.azureAISearchProxy, this.openAIProxy, options),
                                "InformationFinder")
                )
                .withPlugin(
                        KernelPluginFactory.importPluginFromResourcesDirectory(
                                "semantickernel/Plugins",
                                "RAG",
                                "AnswerQuestion",
                                null,
                                String.class
                        )
                )
                .build();
    }
}
