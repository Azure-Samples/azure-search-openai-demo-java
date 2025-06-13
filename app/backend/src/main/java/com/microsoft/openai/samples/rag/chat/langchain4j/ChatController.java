// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.chat.langchain4j;

import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.model.ChatAppRequest;
import com.microsoft.openai.samples.rag.model.ChatAppResponse;
import com.microsoft.openai.samples.rag.model.ResponseMessage;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Controller providing the api to chat with the RAG model.The APPLICATION_NDJSON_VALUE based API is used for streaming the response.
 * Streaming works only with RAG implementation based on plain java Open AI client sdk. Semantic Kernel doesn't support streaming yet
 */
@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);

    private final Langchain4JChatFlow langchain4JChatApproach;

    public ChatController(Langchain4JChatFlow langchain4JChatApproach) {
        this.langchain4JChatApproach = langchain4JChatApproach;

    }


    @PostMapping(value = "/api/chat/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> chatStream(
            @RequestBody ChatAppRequest chatRequest,
            @RequestParam(value = "session_state", required = false) String sessionState) {

        sessionState = getOrCreateSessionState(sessionState);

        LOGGER.info("Received request for async chat api with message {}", chatRequest.messages());


        if (chatRequest.messages() == null || chatRequest.messages().isEmpty()) {
            LOGGER.warn("history cannot be null in Chat request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }


        var ragOptions = new RAGOptions.Builder()
                .retrievialMode(chatRequest.context().overrides().retrieval_mode().name())
                .semanticRanker(chatRequest.context().overrides().semantic_ranker())
                .semanticCaptions(chatRequest.context().overrides().semantic_captions())
                .suggestFollowupQuestions(chatRequest.context().overrides().suggest_followup_questions())
                .excludeCategory(chatRequest.context().overrides().exclude_category())
                .promptTemplate(chatRequest.context().overrides().prompt_template())
                .top(chatRequest.context().overrides().top())
                .minimumRerankerScore(chatRequest.context().overrides().minimum_reranker_score())
                .minimumSearchScore(chatRequest.context().overrides().minimum_search_score())
                .threadId(sessionState)
                .build();


        List<ChatMessage> messages = ChatGPTUtils.convertToLangchain4J(chatRequest.messages());

        StreamingResponseBody response =
                output ->     langchain4JChatApproach.runStreaming(messages, ragOptions, output);

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_NDJSON).body(response);
    }

    @PostMapping(value = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatAppResponse> chat(@RequestBody ChatAppRequest chatRequest,
                                                @RequestParam(value = "session_state", required = false) String sessionState) {
        sessionState = getOrCreateSessionState(sessionState);

        LOGGER.info("Received request for sync chat api with message {}", chatRequest.messages());

        var ragOptions = new RAGOptions.Builder()
                .retrievialMode(chatRequest.context().overrides().retrieval_mode().name())
                .semanticRanker(chatRequest.context().overrides().semantic_ranker())
                .semanticCaptions(chatRequest.context().overrides().semantic_captions())
                .suggestFollowupQuestions(chatRequest.context().overrides().suggest_followup_questions())
                .excludeCategory(chatRequest.context().overrides().exclude_category())
                .promptTemplate(chatRequest.context().overrides().prompt_template())
                .top(chatRequest.context().overrides().top())
                .minimumRerankerScore(chatRequest.context().overrides().minimum_reranker_score())
                .minimumSearchScore(chatRequest.context().overrides().minimum_search_score())
                .threadId(sessionState)
                .build();


        List<ChatMessage> messages = ChatGPTUtils.convertToLangchain4J(chatRequest.messages());
        return ResponseEntity.ok(
                langchain4JChatApproach.run(messages, ragOptions));
    }

    private String getOrCreateSessionState(String sessionState) {
        if (sessionState == null || sessionState.isEmpty()) {
            String newSessionState = UUID.randomUUID().toString();
            LOGGER.info("Generated new session_state: {}", newSessionState);
            return newSessionState;
        } else {
            LOGGER.info("Received session_state: {}", sessionState);
            return sessionState;
        }
    }

}
