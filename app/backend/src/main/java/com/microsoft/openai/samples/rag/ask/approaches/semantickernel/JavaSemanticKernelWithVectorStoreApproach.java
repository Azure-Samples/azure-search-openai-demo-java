// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.core.credential.TokenCredential;
import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.indexes.SearchIndexAsyncClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.chat.approaches.semantickernel.JavaSemanticKernelWithVectorStoreChatApproach;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchVectorStoreApproach;
import com.microsoft.semantickernel.Kernel;

import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.aiservices.openai.textembedding.OpenAITextEmbeddingGenerationService;
import com.microsoft.semantickernel.data.azureaisearch.AzureAISearchVectorStoreRecordCollection;
import com.microsoft.semantickernel.data.azureaisearch.AzureAISearchVectorStoreRecordCollectionOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.plugin.KernelPlugin;
import com.microsoft.semantickernel.plugin.KernelPluginFactory;
import com.microsoft.semantickernel.semanticfunctions.HandlebarsPromptTemplateFactory;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionYaml;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.services.textembedding.EmbeddingGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchVectorStoreApproach.MemoryRecord;

/**
 * Use Java Semantic Kernel framework with built-in VectorStores for embeddings similarity search. A
 * semantic function is defined in RAG.AnswerQuestion (src/main/resources/semantickernel/Plugins) to
 * build the prompt template which is grounded using results from the VectorRecordCollection.
 * An AzureAISearchVectorStoreRecordCollection is used to manage an AzureAISearch index populated by the
 * documents ingestion process.
 */
@Component
public class JavaSemanticKernelWithVectorStoreApproach implements RAGApproach<String, RAGResponse> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(JavaSemanticKernelWithVectorStoreApproach.class);
    private final TokenCredential tokenCredential;
    private final OpenAIAsyncClient openAIAsyncClient;

    private final SearchAsyncClient searchAsyncClient;

    private final String EMBEDDING_FIELD_NAME = "embedding";

    @Value("${cognitive.search.service}")
    String searchServiceName;

    @Value("${cognitive.search.index}")
    String indexName;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    @Value("${openai.embedding.deployment}")
    private String embeddingDeploymentModelId;

    public JavaSemanticKernelWithVectorStoreApproach(
            TokenCredential tokenCredential,
            OpenAIAsyncClient openAIAsyncClient,
            SearchAsyncClient searchAsyncClient) {
        this.tokenCredential = tokenCredential;
        this.openAIAsyncClient = openAIAsyncClient;
        this.searchAsyncClient = searchAsyncClient;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {
        // Build semantic kernel context with AnswerQuestion plugin, EmbeddingGenerationService and ChatCompletionService.
        // skill is imported from src/main/resources/semantickernel/Plugins.
        Kernel semanticKernel = buildSemanticKernel(options);

        // STEP 1: Create Azure AI Search client
        SearchIndexAsyncClient client = new SearchIndexClientBuilder()
                .endpoint("https://%s.search.windows.net".formatted(searchServiceName))
                .credential(tokenCredential)
                .buildAsyncClient();

        // STEP 2: Build Vector Record Collection
        AzureAISearchVectorStoreRecordCollection<MemoryRecord> recordCollection = new AzureAISearchVectorStoreRecordCollection<>(
                client,
                indexName,
                AzureAISearchVectorStoreRecordCollectionOptions.<MemoryRecord>builder()
                        .withRecordClass(MemoryRecord.class)
                        .build()
        );

        // STEP 3: Retrieve relevant documents using user question.
        List<MemoryRecord> memoryResult = AzureAISearchVectorStoreApproach.searchAsync(
                question, semanticKernel, recordCollection, options);

        String sources = AzureAISearchVectorStoreApproach.buildSourcesText(memoryResult);
        List<ContentSource> sourcesList = AzureAISearchVectorStoreApproach.buildSources(memoryResult);

        // STEP 4: Generate a contextual and content specific answer using the search results and question
        KernelFunction<String> answerQuestion = semanticKernel.getFunction("RAG", "AnswerQuestion");
        KernelFunctionArguments arguments = KernelFunctionArguments.builder()
                .withVariable("sources", sourcesList)
                .withVariable("input", question)
                .build();

        FunctionResult<String> reply = answerQuestion.invokeAsync(semanticKernel)
                .withArguments(arguments)
                .block();

        return new RAGResponse.Builder()
                .prompt("Prompt is managed by SK and can't be displayed here. See App logs for"
                        + " prompt")
                .answer(reply.getResult())
                .sources(sourcesList)
                .sourcesAsText(sources)
                .question(question)
                .build();
    }

    @Override
    public void runStreaming(
            String questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    private Kernel buildSemanticKernel(RAGOptions options) {
        KernelPlugin answerPlugin;
        try {
            answerPlugin = KernelPluginFactory.createFromFunctions(
                    "RAG",
                    "AnswerQuestion",
                    List.of(
                            KernelFunctionYaml.fromPromptYaml(
                                    EmbeddedResourceLoader.readFile(
                                            "semantickernel/Plugins/RAG/AnswerQuestion/answerQuestion.prompt.yaml",
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
                .build();

        return kernel;
    }
}
