// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.search.documents.indexes.SearchIndexAsyncClient;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.chat.approaches.semantickernel.JavaSemanticKernelWithVectorStoreChatApproach;
import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchVectorStoreUtils;
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

import com.microsoft.openai.samples.rag.retrieval.semantickernel.AzureAISearchVectorStoreUtils.DocumentRecord;

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
    private final OpenAIAsyncClient openAIAsyncClient;
    private final SearchIndexAsyncClient searchAsyncClient;


    @Value("${cognitive.search.index}")
    String indexName;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    @Value("${openai.embedding.deployment}")
    private String embeddingDeploymentModelId;

    public JavaSemanticKernelWithVectorStoreApproach(
            OpenAIAsyncClient openAIAsyncClient,
            SearchIndexAsyncClient searchAsyncClient) {
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

        // STEP 1: Build Vector Record Collection
        AzureAISearchVectorStoreRecordCollection<DocumentRecord> recordCollection = new AzureAISearchVectorStoreRecordCollection<>(
                searchAsyncClient,
                indexName,
                AzureAISearchVectorStoreRecordCollectionOptions.<DocumentRecord>builder()
                        .withRecordClass(DocumentRecord.class)
                        .build()
        );

        // STEP 2: Retrieve relevant documents using user question.
        List<DocumentRecord> memoryResult = AzureAISearchVectorStoreUtils.searchAsync(
                question, semanticKernel, recordCollection, options);

        String sources = AzureAISearchVectorStoreUtils.buildSourcesText(memoryResult);
        List<ContentSource> sourcesList = AzureAISearchVectorStoreUtils.buildSources(memoryResult);

        // STEP 3: Generate a contextual and content specific answer using the search results and question
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
