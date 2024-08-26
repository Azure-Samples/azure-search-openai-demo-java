// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.chat.approaches.semantickernel.JavaSemanticKernelChainsChatApproach;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.HandlebarsPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionYaml;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Use Java Semantic Kernel framework with semantic and native functions chaining. It uses an
 * imperative style for AI orchestration through sequentially call semantic kernel functions.
 * InformationFinder.SearchFromQuestion native function and RAG.AnswerQuestion semantic function are called
 * sequentially. Several Azure AI Search retrieval options are available: Text, Vector, Hybrid.
 */
@Component
public class JavaSemanticKernelChainsApproach implements RAGApproach<String, RAGResponse> {

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

    @Override
    public RAGResponse run(String question, RAGOptions options) {

        // Build semantic kernel context
        Kernel semanticKernel = buildSemanticKernel(options);

        // STEP 1: Retrieve relevant documents using user question. It reuses the
        // AzureAISearchRetriever approach through the AzureAISearchPlugin native function.
        FunctionResult<List> sources = semanticKernel
                .getPlugin("InformationFinder")
                .get("SearchFromQuestion")
                .invokeAsync(semanticKernel)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withInput(question)
                                .build()
                )
                .withResultType(List.class)
                .block();

        // STEP 2: Build a SK context with the sources retrieved from the memory store and the user
        // question.
        var answerVariables =
                KernelFunctionArguments.builder()
                        .withVariable("sources", sources.getResult())
                        .withVariable("input", question)
                        .build();

        /*
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
                .sources(sources.getResult())
                .sourcesAsText(sources.getResult().get(0).toString())
                .question(question)
                .build();
    }

    @Override
    public void runStreaming(
            String questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    /**
     * Build semantic kernel context with AnswerQuestion semantic function and
     * InformationFinder.SearchFromQuestion native function. AnswerQuestion is imported from
     * src/main/resources/semantickernel/Plugins. InformationFinder.SearchFromQuestion is implemented in a
     * traditional Java class method: AzureAISearchPlugin.searchFromConversation
     */
    private Kernel buildSemanticKernel(RAGOptions options) {
        try {
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
                            KernelPluginFactory.createFromFunctions(
                                    "RAG",
                                    "AnswerQuestion",
                                    List.of(
                                            KernelFunctionYaml.fromPromptYaml(
                                                    EmbeddedResourceLoader.readFile(
                                                            "semantickernel/Plugins/RAG/AnswerQuestion/answerQuestion.prompt.yaml",
                                                            JavaSemanticKernelChainsChatApproach.class,
                                                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                                    ),
                                                    new HandlebarsPromptTemplateFactory())
                                    )
                            )
                    )
                    .build();
        } catch (IOException e) {
            // Failed to read plugin yaml, should not happen
            throw new RuntimeException(e);
        }
    }
}
