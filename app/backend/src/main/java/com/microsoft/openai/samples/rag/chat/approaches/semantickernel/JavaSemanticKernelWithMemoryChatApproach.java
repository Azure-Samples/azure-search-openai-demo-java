package com.microsoft.openai.samples.rag.chat.approaches.semantickernel;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.core.credential.TokenCredential;
import com.azure.search.documents.SearchAsyncClient;
import com.azure.search.documents.SearchDocument;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.memory.CustomAzureCognitiveSearchMemoryStore;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.ai.embeddings.Embedding;
import com.microsoft.semantickernel.memory.MemoryQueryResult;
import com.microsoft.semantickernel.memory.MemoryRecord;
import com.microsoft.semantickernel.orchestration.SKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Accomplish the same task as in the PlainJavaAskApproach approach but using Semantic Kernel framework:
 * 1. Memory abstraction is used for vector search capability. It uses Azure Cognitive Search as memory store.
 * 2. Semantic function has been defined to ask question using sources from memory search results
 */
@Component
public class JavaSemanticKernelWithMemoryChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaSemanticKernelWithMemoryChatApproach.class);
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

    public JavaSemanticKernelWithMemoryChatApproach(TokenCredential tokenCredential, OpenAIAsyncClient openAIAsyncClient, SearchAsyncClient searchAsyncClient) {
        this.tokenCredential = tokenCredential;
        this.openAIAsyncClient = openAIAsyncClient;
        this.searchAsyncClient = searchAsyncClient;
    }

    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {

        String question = ChatGPTUtils.getLastUserQuestion(questionOrConversation.getMessages());

        //Build semantic kernel with Azure Cognitive Search as memory store. AnswerQuestion skill is imported from resources.
        Kernel semanticKernel = buildSemanticKernel(options);

        /**
         * Use semantic kernel built-in memory.searchAsync. It uses OpenAI to generate embeddings for the provided question.
         * Question embeddings are provided to cognitive search via search options.
         */
        List<MemoryQueryResult> memoryResult = semanticKernel.getMemory().searchAsync(
                        indexName,
                        question,
                        options.getTop(),
                        0.5f,
                        false)
                .block();

        LOGGER.info("Total {} sources found in cognitive vector store for search query[{}]", memoryResult.size(), question);

        String sources = buildSourcesText(memoryResult);
        List<ContentSource> sourcesList = buildSources(memoryResult);

        SKContext skcontext = SKBuilders.context().build()
                .setVariable("sources", sources)
                .setVariable("input", question);


        Mono<SKContext> result = semanticKernel.getFunction("RAG", "AnswerQuestion").invokeAsync(skcontext);

        return new RAGResponse.Builder()
                //.prompt(plan.toPlanString())
                .prompt("placeholders for prompt")
                .answer(result.block().getResult())
                .sources(sourcesList)
                .sourcesAsText(sources)
                .question(question)
                .build();

    }

    @Override
    public void runStreaming(ChatGPTConversation questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new IllegalStateException("Streaming not supported for this approach");
    }

    private List<ContentSource> buildSources(List<MemoryQueryResult> memoryResult) {
        return memoryResult
                .stream()
                .map(result -> {
                    return new ContentSource(
                            result.getMetadata().getId(),
                            result.getMetadata().getText()
                    );
                })
                .collect(Collectors.toList());
    }

    private String buildSourcesText(List<MemoryQueryResult> memoryResult) {
        StringBuilder sourcesContentBuffer = new StringBuilder();
        memoryResult.stream().forEach(memory -> {
            sourcesContentBuffer.append(memory.getMetadata().getId())
                    .append(": ")
                    .append(memory.getMetadata().getText().replace("\n", ""))
                    .append("\n");
        });
        return sourcesContentBuffer.toString();
    }

    private Kernel buildSemanticKernel(RAGOptions options) {
        var kernelWithACS = SKBuilders.kernel()
                .withMemoryStorage(
                        new CustomAzureCognitiveSearchMemoryStore("https://%s.search.windows.net".formatted(searchServiceName),
                                tokenCredential,
                                this.searchAsyncClient,
                                this.EMBEDDING_FIELD_NAME,
                                buildCustomMemoryMapper()))
                .withDefaultAIService(SKBuilders.textEmbeddingGeneration()
                        .withOpenAIClient(openAIAsyncClient)
                        .withModelId(embeddingDeploymentModelId)
                        .build())
                .withDefaultAIService(SKBuilders.chatCompletion()
                        .withModelId(gptChatDeploymentModelId)
                        .withOpenAIClient(this.openAIAsyncClient)
                        .build())
                .build();

        kernelWithACS.importSkillFromResources("semantickernel/Plugins", "RAG", "AnswerQuestion", null);
        return kernelWithACS;
    }

    private Function<SearchDocument, MemoryRecord> buildCustomMemoryMapper() {
        return searchDocument -> {
            return MemoryRecord.localRecord(
                    (String) searchDocument.get("sourcepage"),
                    (String) searchDocument.get("content"),
                    "chunked text from original source",
                    new Embedding((List<Float>) searchDocument.get(EMBEDDING_FIELD_NAME)),
                    (String) searchDocument.get("category"),
                    (String) searchDocument.get("id"),
                    null);

        };
    }
}
