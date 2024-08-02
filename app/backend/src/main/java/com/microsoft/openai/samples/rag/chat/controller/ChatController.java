// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.chat.controller;

import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproachFactory;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.approaches.RAGType;
import com.microsoft.openai.samples.rag.approaches.SemanticKernelMode;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;
import com.microsoft.openai.samples.rag.controller.ChatAppRequest;
import com.microsoft.openai.samples.rag.controller.ChatResponse;
import com.microsoft.openai.samples.rag.controller.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Controller providing the api to chat with the RAG model.The APPLICATION_NDJSON_VALUE based API is used for streaming the response.
 * Streaming works only with RAG implementation based on plain java Open AI client sdk. Semantic Kernel doesn't support streaming yet
 */
@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private final RAGApproachFactory<ChatGPTConversation, RAGResponse> ragApproachFactory;

    public ChatController(RAGApproachFactory<ChatGPTConversation, RAGResponse> ragApproachFactory) {
        this.ragApproachFactory = ragApproachFactory;
    }

    @PostMapping(value = "/api/chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> openAIAskStream(
            @RequestBody ChatAppRequest chatRequest) {
        if (!chatRequest.stream()) {
            LOGGER.warn(
                    "Requested a content-type of application/ndjson however did not requested"
                            + " streaming. Please use a content-type of application/json");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested a content-type of application/ndjson however did not requested"
                            + " streaming. Please use a content-type of application/json");
        }

        LOGGER.info("Received request for chat api with approach[{}]", chatRequest.approach());

        if (!StringUtils.hasText(chatRequest.approach())) {
            LOGGER.warn("approach cannot be null in CHAT request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (chatRequest.messages() == null || chatRequest.messages().isEmpty()) {
            LOGGER.warn("history cannot be null in Chat request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var semanticKernelMode = chatRequest.context().overrides().semantic_kernel_mode();
        if (semanticKernelMode == null) {
            semanticKernelMode = SemanticKernelMode.chains.name();
        }

        var ragOptions = new RAGOptions.Builder()
                .retrievialMode(chatRequest.context().overrides().retrieval_mode().name())
                .semanticRanker(chatRequest.context().overrides().semantic_ranker())
                .semanticCaptions(chatRequest.context().overrides().semantic_captions())
                .suggestFollowupQuestions(chatRequest.context().overrides().suggest_followup_questions())
                .excludeCategory(chatRequest.context().overrides().exclude_category())
                .promptTemplate(chatRequest.context().overrides().prompt_template())
                .top(chatRequest.context().overrides().top())
                .semanticKernelMode(semanticKernelMode)
                .build();

        RAGApproach<ChatGPTConversation, RAGResponse> ragApproach =
                ragApproachFactory.createApproach(chatRequest.approach(), RAGType.CHAT, ragOptions);

        ChatGPTConversation chatGPTConversation = convertToChatGPT(chatRequest.messages());

        StreamingResponseBody response =
                output -> {
                    try {
                        ragApproach.runStreaming(chatGPTConversation, ragOptions, output);
                    } finally {
                        output.flush();
                        output.close();
                    }
                };

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_NDJSON).body(response);
    }

    @PostMapping(value = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> openAIAsk(@RequestBody ChatAppRequest chatRequest) {
        if (chatRequest.stream()) {
            LOGGER.warn(
                    "Requested a content-type of application/json however also requested streaming."
                            + " Please use a content-type of application/ndjson");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested a content-type of application/json however also requested streaming."
                            + " Please use a content-type of application/ndjson");
        }

        LOGGER.info("Received request for chat api with approach[{}]", chatRequest.approach());

        if (!StringUtils.hasText(chatRequest.approach())) {
            LOGGER.warn("approach cannot be null in CHAT request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (chatRequest.messages() == null || chatRequest.messages().isEmpty()) {
            LOGGER.warn("history cannot be null in Chat request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var semanticKernelMode = chatRequest.context().overrides().semantic_kernel_mode();
        if (semanticKernelMode == null) {
            semanticKernelMode = SemanticKernelMode.chains.name();
        }

        var ragOptions = new RAGOptions.Builder()
                .retrievialMode(chatRequest.context().overrides().retrieval_mode().name())
                .semanticRanker(chatRequest.context().overrides().semantic_ranker())
                .semanticCaptions(chatRequest.context().overrides().semantic_captions())
                .suggestFollowupQuestions(chatRequest.context().overrides().suggest_followup_questions())
                .excludeCategory(chatRequest.context().overrides().exclude_category())
                .promptTemplate(chatRequest.context().overrides().prompt_template())
                .top(chatRequest.context().overrides().top())
                .semanticKernelMode(semanticKernelMode)
                .build();

        RAGApproach<ChatGPTConversation, RAGResponse> ragApproach =
                ragApproachFactory.createApproach(chatRequest.approach(), RAGType.CHAT, ragOptions);

        ChatGPTConversation chatGPTConversation = convertToChatGPT(chatRequest.messages());
        return ResponseEntity.ok(
                ChatResponse.buildChatResponse(ragApproach.run(chatGPTConversation, ragOptions)));
    }

    private ChatGPTConversation convertToChatGPT(List<ResponseMessage> chatHistory) {
        return new ChatGPTConversation(
                chatHistory.stream()
                        .map(
                                historyChat -> {
                                    List<ChatGPTMessage> chatGPTMessages = new ArrayList<>();
                                    chatGPTMessages.add(
                                            new ChatGPTMessage(
                                                    ChatGPTMessage.ChatRole.fromString(
                                                            historyChat.role()),
                                                    historyChat.content()));
                                    return chatGPTMessages;
                                })
                        .flatMap(Collection::stream)
                        .toList());
    }
}
