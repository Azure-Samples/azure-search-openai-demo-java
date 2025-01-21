package com.microsoft.openai.samples.rag.chat.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.search.documents.indexes.SearchIndexAsyncClient;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchVectorStoreUtils;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.aiservices.openai.textembedding.OpenAITextEmbeddingGenerationService;
import com.microsoft.semantickernel.data.azureaisearch.AzureAISearchVectorStoreRecordCollection;
import com.microsoft.semantickernel.data.azureaisearch.AzureAISearchVectorStoreRecordCollectionOptions;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.HandlebarsPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionYaml;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import com.microsoft.semantickernel.services.textembedding.EmbeddingGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchVectorStoreUtils.DocumentRecord;

/**
 * Use Java Semantic Kernel framework with built-in VectorStores for embeddings similarity search. A
 * semantic function is defined in RAG.AnswerConversation (src/main/resources/semantickernel/Plugins) to
 * build the prompt template which is grounded using results from the VectorRecordCollection.
 * An AzureAISearchVectorStoreRecordCollection instance is used to manage an AzureAISearch index populated by the
 * documents ingestion process.
 */
@Component
public class JavaSemanticKernelWithVectorStoreChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSemanticKernelWithVectorStoreChatApproach.class);
    private final OpenAIAsyncClient openAIAsyncClient;
    private final SearchIndexAsyncClient searchAsyncClient;
    private String renderedConversation;

    @Value("${cognitive.search.index}")
    String indexName;
    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;
    @Value("${openai.embedding.deployment}")
    private String embeddingDeploymentModelId;

    public JavaSemanticKernelWithVectorStoreChatApproach(OpenAIAsyncClient openAIAsyncClient, SearchIndexAsyncClient searchAsyncClient) {
        this.openAIAsyncClient = openAIAsyncClient;
        this.searchAsyncClient = searchAsyncClient;
    }

    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {
        ChatHistory conversation = questionOrConversation.toSKChatHistory();
        ChatMessageContent<?> question = conversation.getLastMessage().get();

        // Build semantic kernel context with AnswerConversation and ExtractKeywords plugins, EmbeddingGenerationService and ChatCompletionService.
        Kernel semanticKernel = buildSemanticKernel();

        // STEP 1: Build Vector Record Collection
        AzureAISearchVectorStoreRecordCollection<DocumentRecord> recordCollection = new AzureAISearchVectorStoreRecordCollection<>(
                searchAsyncClient,
                indexName,
                AzureAISearchVectorStoreRecordCollectionOptions.<DocumentRecord>builder()
                        .withRecordClass(DocumentRecord.class)
                        .build()
        );

        // STEP 2: Retrieve relevant documents using keywords extracted from the chat history
        String conversationString = ChatGPTUtils.formatAsChatML(questionOrConversation.toOpenAIChatMessages());
        List<DocumentRecord> sourcesResult = getSourcesFromConversation(conversationString, semanticKernel, recordCollection, options);

        LOGGER.info("Total {} sources found in cognitive vector store for search query[{}]", sourcesResult.size(), question);

        String sources = AzureAISearchVectorStoreUtils.buildSourcesText(sourcesResult);
        List<ContentSource> sourcesList = AzureAISearchVectorStoreUtils.buildSources(sourcesResult);

        // STEP 3: Generate a contextual and content specific answer using the search results and chat history
        KernelFunction<String> answerConversation = semanticKernel.getFunction("RAG", "AnswerConversation");
        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("sources", sourcesList)
                .withVariable("conversation", removeLastMessage(conversation))
                .withVariable("suggestions", options.isSuggestFollowupQuestions())
                .withVariable("input", question.getContent())
                .build();

        FunctionResult<String> reply = answerConversation.invokeAsync(semanticKernel)
                .withArguments(arguments)
                .block();

        return new RAGResponse.Builder()
                .prompt(renderedConversation)
                .answer(reply.getResult())
                .sources(sourcesList)
                .sourcesAsText(sources)
                .question(question.getContent())
                .build();
    }

    @Override
    public void runStreaming(ChatGPTConversation questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    private ChatHistory removeLastMessage(ChatHistory conversation) {
        ArrayList<ChatMessageContent<?>> messages = new ArrayList<>(conversation.getMessages());
        messages.remove(conversation.getMessages().size() - 1);
        return new ChatHistory(messages);
    }

    private List<DocumentRecord> getSourcesFromConversation(String conversation,
                                                            Kernel kernel,
                                                            AzureAISearchVectorStoreRecordCollection<DocumentRecord> recordCollection,
                                                            RAGOptions ragOptions) {
        KernelFunction<String> extractKeywords = kernel
                .getPlugin("RAG")
                .get("ExtractKeywords");

        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("conversation", conversation)
                .build();

        FunctionResult<String> result = extractKeywords
                .invokeAsync(kernel)
                .withArguments(arguments)
                .block();
        String searchQuery = result.getResult();

        return AzureAISearchVectorStoreUtils.searchAsync(
                searchQuery,
                kernel,
                recordCollection,
                ragOptions
        );
    }

    private Kernel buildSemanticKernel() {
        KernelPlugin answerPlugin, extractKeywordsPlugin;
        try {
            answerPlugin = KernelPluginFactory.createFromFunctions(
                    "RAG",
                    "AnswerConversation",
                    List.of(
                            KernelFunctionYaml.fromPromptYaml(
                                    EmbeddedResourceLoader.readFile(
                                            "semantickernel/Plugins/RAG/AnswerConversation/answerConversation.prompt.yaml",
                                            JavaSemanticKernelWithVectorStoreChatApproach.class,
                                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                    ),
                                    new HandlebarsPromptTemplateFactory())
                    )
            );
            extractKeywordsPlugin = KernelPluginFactory.createFromFunctions(
                    "RAG",
                    "ExtractKeywords",
                    List.of(
                            KernelFunctionYaml.fromPromptYaml(
                                    EmbeddedResourceLoader.readFile(
                                            "semantickernel/Plugins/RAG/ExtractKeywords/extractKeywords.prompt.yaml",
                                            JavaSemanticKernelWithVectorStoreChatApproach.class,
                                            EmbeddedResourceLoader.ResourceLocation.CLASSPATH_ROOT
                                    ),
                                    new HandlebarsPromptTemplateFactory())
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Kernel kernel = Kernel.builder()
                .withAIService(EmbeddingGenerationService.class, OpenAITextEmbeddingGenerationService.builder()
                        .withOpenAIAsyncClient(openAIAsyncClient)
                        .withModelId(embeddingDeploymentModelId)
                        .withDimensions(1536)
                        .build())
                .withAIService(ChatCompletionService.class, OpenAIChatCompletion.builder()
                        .withOpenAIAsyncClient(this.openAIAsyncClient)
                        .withModelId(gptChatDeploymentModelId)
                        .build())
                .withPlugin(answerPlugin)
                .withPlugin(extractKeywordsPlugin)
                .build();

        kernel.getGlobalKernelHooks().addPreChatCompletionHook(event -> {
            this.renderedConversation = ChatGPTUtils.formatAsChatML(event.getOptions().getMessages());
            return event;
        });

        return kernel;
    }
}
