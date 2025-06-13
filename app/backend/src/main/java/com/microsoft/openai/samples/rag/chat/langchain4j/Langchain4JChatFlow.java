// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.chat.langchain4j;

import com.azure.core.credential.TokenCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.common.ResponseMessageUtils;

import com.microsoft.openai.samples.rag.model.ChatAppResponse;
import com.microsoft.openai.samples.rag.security.LoggedUserService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchContentRetriever;
import dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchQueryType;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Simple chat-read-retrieve-read java implementation, using the Cognitive Search and OpenAI APIs
 * directly. It first calls OpenAI to generate a search keyword for the chat history and then answer
 * to the last chat question. Several cognitive search retrieval options are available: Text,
 * Vector, Hybrid. When Hybrid and Vector are selected an additional call to OpenAI is required to
 * generate embeddings vector for the chat extracted keywords.
 */
@Component
public class Langchain4JChatFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4JChatFlow.class);
    private final ObjectMapper objectMapper;
    private final LoggedUserService loggedUserService;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final EmbeddingModel embeddingModel;
    private final TokenCredential tokenCredential;

    @Value("${azureai.search.service}")
    String searchServiceName;

    @Value("${azureai.search.index}")
    String indexName;

    @Value("${openai.embedding.dimension}")
    int dimensions;

    @Value("${app.enableGlobalDocumentAccess:true}")
    boolean enableGlobalDocumentAccess;

    public Langchain4JChatFlow(
            ChatModel chatModel,
            EmbeddingModel embeddingModel,
            ObjectMapper objectMapper,
            LoggedUserService loggedUserService,
            StreamingChatModel streamingChatModel,
            TokenCredential tokenCredential) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.loggedUserService = loggedUserService;
        this.streamingChatModel = streamingChatModel;
        this.tokenCredential = tokenCredential;
    }

    /**
     * @param conversationHistory the chat history messages, including the last question or conversation
     * @param options the RAG options to configure the retrieval and response generation
     * @return
     */

    public ChatAppResponse run(List<ChatMessage> conversationHistory, RAGOptions options) {

        String userId = this.loggedUserService.getLoggedUser().entraId();

        // STEP 1: Extract keywords from the chat history to use for retrieval.
        var extractKeywordsAssistant = new ExtractKeywordsAgent(this.chatModel, conversationHistory);
        var keywords = extractKeywordsAssistant.extractKeywords();
        LOGGER.info("Extracted keywords for retrieval: {}", keywords);

        // STEP 2: Retrieve relevant documents using keywords extracted from the chat history.
        ContentRetriever contentRetriever = buildContentRetriever(options, userId);
        var sources = contentRetriever.retrieve(Query.from(keywords));
        LOGGER.info("Total {} sources retrieved", sources.size());

        // STEP 3: Generate a contextual and content specific answer using the search results and chat history
        UserMessage userQuestion = (UserMessage)conversationHistory.get(conversationHistory.size() - 1);
        conversationHistory.remove(conversationHistory.size() - 1);

        var answerQuestionAgent = new AnswerQuestionAgent(
                conversationHistory,
                sources,
                options.getPromptTemplate(),
                options.isSuggestFollowupQuestions(),
                this.chatModel,
                this.streamingChatModel
                );

        var chatResponse = answerQuestionAgent.answerQuestion(userQuestion.singleText(), options);
        

        LOGGER.info(
                "Chat completion generated with Input Tokens[{}], Completions Tokens[{}], Total"
                        + " Tokens[{}]",
                chatResponse.tokenUsage().inputTokenCount(),
                chatResponse.tokenUsage().outputTokenCount(),
                chatResponse.tokenUsage().totalTokenCount());

        return ResponseMessageUtils.buildChatResponse(answerQuestionAgent.getMessages()
        , options, sources,chatResponse,keywords);
    }



    public void runStreaming(
            List<ChatMessage> conversationHistory,
            RAGOptions options,
            OutputStream outputStream) {

        String userId = this.loggedUserService.getLoggedUser().entraId();

        // STEP 1: Extract keywords from the chat history to use for retrieval.
        var extractKeywordsAssistant = new ExtractKeywordsAgent(this.chatModel, conversationHistory);
        var keywords = extractKeywordsAssistant.extractKeywords();
        LOGGER.info("Extracted keywords for retrieval: {}", keywords);

        // STEP 2: Retrieve relevant documents using keywords extracted from the chat history.
        ContentRetriever contentRetriever = buildContentRetriever(options, userId);
        var sources = contentRetriever.retrieve(Query.from(keywords));
        LOGGER.info("Total {} sources retrieved", sources.size());

        // STEP 3: Generate a contextual and content specific answer using the search results and chat history
        UserMessage userQuestion = (UserMessage)conversationHistory.get(conversationHistory.size() - 1);
        conversationHistory.remove(conversationHistory.size() - 1);

        var answerQuestionAgent = new AnswerQuestionAgent(
                conversationHistory,
                sources,
                options.getPromptTemplate(),
                options.isSuggestFollowupQuestions(),
                this.chatModel,
                this.streamingChatModel
        );

        var streamingResponseHandler = new StreamingChatResponseHandler() {
            private int index = 0;
            @Override
            public void onPartialResponse(String partialResponse) {

                ChatAppResponse chatAppResponse;
                if (index == 0)
                  chatAppResponse = ResponseMessageUtils.buildDelta0(options, sources, partialResponse);
                 else
                  chatAppResponse = ResponseMessageUtils.buildDelta(partialResponse);

                 index++;

                try {
                    String value = objectMapper.writeValueAsString(chatAppResponse) + "\n";
                    outputStream.write(value.getBytes());
                    outputStream.flush();
                } catch (Exception e) {
                    LOGGER.warn("Error while trying to send delta chat response to the stream",e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
               var chatAppResponse = ResponseMessageUtils.buildDeltaComplete(answerQuestionAgent.getMessages()
                        , options, sources,chatResponse,keywords);

                try {
                    String value = objectMapper.writeValueAsString(chatAppResponse) + "\n";
                    outputStream.write(value.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    LOGGER.warn("Error while trying to send last chat response to the stream",e);
                }
            }

            @Override
            public void onError(Throwable error) {
                // Implement error handling here
                throw new RuntimeException("Error during streaming response", error);
            }

        };

        answerQuestionAgent.answerQuestionStream(userQuestion.singleText(), options, streamingResponseHandler);
        }


    private ContentRetriever buildContentRetriever(RAGOptions options, String userId) {
        AzureAiSearchQueryType queryType;

        switch (options.getRetrievalMode())
        {
            case text:
                queryType = AzureAiSearchQueryType.FULL_TEXT;
                break;
            case hybrid:
                if(options.isSemanticRanker())
                    queryType = AzureAiSearchQueryType.HYBRID_WITH_RERANKING;
                else
                    queryType = AzureAiSearchQueryType.HYBRID;
                break;
            case vectors:
                queryType = AzureAiSearchQueryType.VECTOR;
                break;
            default:
                throw new IllegalArgumentException("Unsupported retrieval mode: " + options.getRetrievalMode());
        }

        String endpoint = "https://%s.search.windows.net".formatted(searchServiceName);

       var builder =  AzureAiSearchContentRetriever.builder()
               .endpoint(endpoint)
               .indexName(this.indexName)
               .tokenCredential(this.tokenCredential)
               .embeddingModel(this.embeddingModel)
               .maxResults(options.getTop())
               .minScore(options.getMinimumSearchScore())
               .queryType(queryType)
               .dimensions(this.dimensions)
               .createOrUpdateIndex(false);

       if ((userId == null || userId.isEmpty()) &&
            (options.getExcludeCategory() == null || options.getExcludeCategory().isEmpty())) {
            return builder.build();
        }

        if ((userId == null || userId.isEmpty()) &&
                (options.getExcludeCategory() != null && !options.getExcludeCategory().isEmpty())) {
            builder.filter(metadataKey("category").isNotEqualTo(options.getExcludeCategory()));
            return builder.build();
        }

        if(enableGlobalDocumentAccess &&
                (userId != null && !userId.isEmpty()) &&
                (options.getExcludeCategory() == null || options.getExcludeCategory().isEmpty())) {
            // If global access is enabled, we can retrieve documents in default user folder
            builder.filter(
                     metadataKey("oid").isEqualTo(userId)
                    .or(metadataKey("oid").isEqualTo("default")));
            return builder.build();
        }

        if(enableGlobalDocumentAccess &&
                (userId != null && !userId.isEmpty()) &&
                (options.getExcludeCategory() != null && !options.getExcludeCategory().isEmpty())) {
            // If global access is enabled, we can retrieve documents in default user folder
            builder.filter(
                    metadataKey("category").isNotEqualTo(options.getExcludeCategory()).and(
                    metadataKey("oid").isEqualTo(userId)
                    .or(metadataKey("oid").isEqualTo("default"))));
            return builder.build();
        }

        if ((userId != null && !userId.isEmpty()) &&
                (options.getExcludeCategory() != null && !options.getExcludeCategory().isEmpty())) {
            builder.filter(metadataKey("category").isNotEqualTo(options.getExcludeCategory()).and(
                           metadataKey("oid").isEqualTo(userId)));
            return builder.build();
        }

        if ((userId != null && !userId.isEmpty()) &&
                (options.getExcludeCategory() == null || options.getExcludeCategory().isEmpty())) {
            builder.filter(metadataKey("oid").isEqualTo(userId));
            return builder.build();
        }



        return builder.build();
    }
}
