package com.microsoft.openai.samples.rag.chat.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.CognitiveSearchPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
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
 * imperative style for AI orchestration through semantic kernel functions chaining.
 * InformationFinder.SearchFromConversation native function and RAG.AnswerConversation semantic function are called
 * sequentially. Several cognitive search retrieval options are available: Text, Vector, Hybrid.
 */
@Component
public class JavaSemanticKernelChainsChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {
    private final CognitiveSearchProxy cognitiveSearchProxy;

    private final OpenAIProxy openAIProxy;

    private final OpenAIAsyncClient openAIAsyncClient;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public JavaSemanticKernelChainsChatApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIAsyncClient openAIAsyncClient, OpenAIProxy openAIProxy) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.openAIAsyncClient = openAIAsyncClient;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param questionOrConversation
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {
        String question = ChatGPTUtils.getLastUserQuestion(questionOrConversation.getMessages());
        String conversation = ChatGPTUtils.formatAsChatML(questionOrConversation.toOpenAIChatMessages());

        Kernel semanticKernel = buildSemanticKernel(options);

        // STEP 1: Retrieve relevant documents using the current conversation. It reuses the
        // CognitiveSearchRetriever approach through the CognitiveSearchPlugin native function.
        FunctionResult<String> searchContext = semanticKernel
                .invokeAsync("InformationFinder", "SearchFromConversation")
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withVariable("conversation", conversation)
                                .build())
                .withResultType(String.class)
                .block();

        // STEP 2: Build a SK context with the sources retrieved from the memory store and conversation
        KernelFunctionArguments variables = KernelFunctionArguments.builder()
                .withVariable("sources", searchContext.getResult())
                .withVariable("conversation", conversation)
                .withVariable("suggestions", String.valueOf(options.isSuggestFollowupQuestions()))
                .withVariable("input", question)
                .build();

        /**
         * STEP 3: Get a reference of the semantic function [AnswerConversation] of the [RAG] plugin
         * (a.k.a. skill) from the SK skills registry and provide it with the pre-built context.
         * Triggering Open AI to get a reply.
         */
        FunctionResult<String> reply = semanticKernel
                .invokeAsync("RAG", "AnswerConversation")
                .withArguments(variables)
                .withResultType(String.class)
                .block();

        return new RAGResponse.Builder()
                .prompt("Prompt is managed by Semantic Kernel")
                .answer(reply.getResult())
                .sources(formSourcesList(searchContext.getResult()))
                .sourcesAsText(searchContext.getResult())
                .question(question)
                .build();
    }

    @Override
    public void runStreaming(
            ChatGPTConversation questionOrConversation,
            RAGOptions options,
            OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    private List<ContentSource> formSourcesList(String result) {
        if (result == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(result
                        .split("\n"))
                .map(source -> {
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
     * Build semantic kernel context with AnswerConversation semantic function and
     * InformationFinder.SearchFromConversation native function. AnswerConversation is imported from
     * src/main/resources/semantickernel/Plugins. InformationFinder.SearchFromConversation is implemented in a
     * traditional Java class method: CognitiveSearchPlugin.searchFromConversation
     *
     * @param options
     * @return
     */
    private Kernel buildSemanticKernel(RAGOptions options) {
        ChatCompletionService chatCompletion = OpenAIChatCompletion.builder()
                .withOpenAIAsyncClient(openAIAsyncClient)
                .withModelId(gptChatDeploymentModelId)
                .build();

        KernelPlugin searchPlugin = KernelPluginFactory.createFromObject(
                new CognitiveSearchPlugin(this.cognitiveSearchProxy, this.openAIProxy, options),
                "InformationFinder");

        KernelPlugin answerPlugin = KernelPluginFactory.importPluginFromResourcesDirectory(
                "semantickernel/Plugins",
                "RAG",
                "AnswerConversation",
                null,
                String.class);

        Kernel kernel = Kernel.builder()
                .withAIService(ChatCompletionService.class, chatCompletion)
                .withPlugin(searchPlugin)
                .withPlugin(answerPlugin)
                .build();

        return kernel;
    }

}
