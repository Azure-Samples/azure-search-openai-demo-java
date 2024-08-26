package com.microsoft.openai.samples.rag.chat.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchPlugin;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.HandlebarsPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionYaml;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Use Java Semantic Kernel framework with semantic and native functions chaining. It uses an
 * imperative style for AI orchestration through semantic kernel functions chaining.
 * InformationFinder.SearchFromConversation native function and RAG.AnswerConversation semantic function are called
 * sequentially. Several cognitive search retrieval options are available: Text, Vector, Hybrid.
 */
@Component
public class JavaSemanticKernelChainsChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {
    private final AzureAISearchProxy azureAISearchProxy;

    private final OpenAIProxy openAIProxy;

    private final OpenAIAsyncClient openAIAsyncClient;

    private String renderedConversation;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public JavaSemanticKernelChainsChatApproach(AzureAISearchProxy azureAISearchProxy, OpenAIAsyncClient openAIAsyncClient, OpenAIProxy openAIProxy) {
        this.azureAISearchProxy = azureAISearchProxy;
        this.openAIAsyncClient = openAIAsyncClient;
        this.openAIProxy = openAIProxy;
    }

    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {
        ChatHistory conversation = questionOrConversation.toSKChatHistory();
        ChatMessageContent<?> question = conversation.getLastMessage().get();

        Kernel semanticKernel = buildSemanticKernel(options);

        // STEP 1: Retrieve relevant documents using the current conversation. It reuses the
        // AzureAISearchRetriever approach through the AzureAISearchPlugin native function.
        FunctionResult<List> searchContext = semanticKernel
                .invokeAsync("InformationFinder", "SearchFromConversation")
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withVariable("conversation", conversation)
                                .build())
                .withResultType(List.class)
                .block();

        // STEP 2: Build a SK context with the sources retrieved from the memory store and conversation
        KernelFunctionArguments variables = KernelFunctionArguments.builder()
                .withVariable("sources", searchContext.getResult())
                .withVariable("conversation", removeLastMessage(conversation))
                .withVariable("suggestions", options.isSuggestFollowupQuestions())
                .withVariable("input", question.getContent())
                .build();

        /*
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
                .prompt(renderedConversation)
                .answer(reply.getResult())
                .sources(searchContext.getResult())
                .sourcesAsText(searchContext.getResult().get(0).toString())
                .question(question.getContent())
                .build();
    }

    private ChatHistory removeLastMessage(ChatHistory conversation) {
        ArrayList<ChatMessageContent<?>> messages = new ArrayList<>(conversation.getMessages());
        messages.remove(conversation.getMessages().size() - 1);
        return new ChatHistory(messages);
    }

    @Override
    public void runStreaming(
            ChatGPTConversation questionOrConversation,
            RAGOptions options,
            OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    /**
     * Build semantic kernel context with AnswerConversation semantic function and
     * InformationFinder.SearchFromConversation native function. AnswerConversation is imported from
     * src/main/resources/semantickernel/Plugins. InformationFinder.SearchFromConversation is implemented in a
     * traditional Java class method: AzureAISearchPlugin.searchFromConversation
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
                new AzureAISearchPlugin(this.azureAISearchProxy, this.openAIProxy, options),
                "InformationFinder");

        KernelPlugin answerPlugin;
        try {
            answerPlugin = KernelPluginFactory.createFromFunctions(
                    "RAG",
                    "AnswerConversation",
                    List.of(
                            KernelFunctionYaml.fromPromptYaml(
                                    EmbeddedResourceLoader.readFile(
                                            "semantickernel/Plugins/RAG/AnswerConversation/answerConversation.prompt.yaml",
                                            JavaSemanticKernelChainsChatApproach.class,
                                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                    ),
                                    new HandlebarsPromptTemplateFactory())
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Kernel kernel = Kernel.builder()
                .withAIService(ChatCompletionService.class, chatCompletion)
                .withPlugin(searchPlugin)
                .withPlugin(answerPlugin)
                .build();

        kernel.getGlobalKernelHooks().addPreChatCompletionHook(event -> {
            this.renderedConversation = ChatGPTUtils.formatAsChatML(event.getOptions().getMessages());
            return event;
        });
        return kernel;
    }

}
