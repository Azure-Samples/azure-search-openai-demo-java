// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.controller;

import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproachFactory;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.approaches.RAGType;
import com.microsoft.openai.samples.rag.controller.ChatAppRequest;
import com.microsoft.openai.samples.rag.controller.ChatResponse;
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

/**
 * Controller providing the api for one shot ask to the RAG model.The APPLICATION_NDJSON_VALUE based API is used for streaming the response.
 * Streaming works only with RAG implementation based on plain java Open AI client sdk. Semantic Kernel doesn't support streaming yet
 */
@RestController
public class AskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AskController.class);
    private final RAGApproachFactory<String, RAGResponse> ragApproachFactory;

    AskController(RAGApproachFactory<String, RAGResponse> ragApproachFactory) {
        this.ragApproachFactory = ragApproachFactory;
    }

    @PostMapping(value = "/api/ask", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity openAIAskStream(@RequestBody ChatAppRequest askRequest) {
        if (!askRequest.stream()) {
            LOGGER.warn(
                    "Requested a content-type of application/ndjson however did not requested"
                            + " streaming. Please use a content-type of application/json");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested a content-type of application/ndjson however did not requested"
                            + " streaming. Please use a content-type of application/json");
        }

        String question = askRequest.messages().get(askRequest.messages().size() - 1).content();
        LOGGER.info(
                "Received request for ask api with question [{}] and approach[{}]",
                question,
                askRequest.approach());

        if (!StringUtils.hasText(askRequest.approach())) {
            LOGGER.warn("approach cannot be null in ASK request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!StringUtils.hasText(question)) {
            LOGGER.warn("question cannot be null in ASK request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var ragOptions =
                new RAGOptions.Builder()
                        .retrievialMode(askRequest.context().overrides().retrieval_mode().name())
                        .semanticKernelMode(askRequest.context().overrides().semantic_kernel_mode())
                        .semanticRanker(askRequest.context().overrides().semantic_ranker())
                        .semanticCaptions(askRequest.context().overrides().semantic_captions())
                        .excludeCategory(askRequest.context().overrides().exclude_category())
                        .promptTemplate(askRequest.context().overrides().prompt_template())
                        .top(askRequest.context().overrides().top())
                        .build();

        RAGApproach<String, RAGResponse> ragApproach =
                ragApproachFactory.createApproach(askRequest.approach(), RAGType.ASK, ragOptions);

        StreamingResponseBody response =
                output -> {
                    try {
                        ragApproach.runStreaming(question, ragOptions, output);
                    } finally {
                        output.flush();
                        output.close();
                    }
                };

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_NDJSON).body(response);
    }

    @PostMapping("/api/ask")
    public ResponseEntity<ChatResponse> openAIAsk(@RequestBody ChatAppRequest askRequest) {
        if (askRequest.stream()) {
            LOGGER.warn(
                    "Requested a content-type of application/json however also requested streaming."
                            + " Please use a content-type of application/ndjson");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Requested a content-type of application/json however also requested streaming."
                            + " Please use a content-type of application/ndjson");
        }

        String question = askRequest.messages().get(askRequest.messages().size() - 1).content();
        LOGGER.info(
                "Received request for ask api with question [{}] and approach[{}]",
                question,
                askRequest.approach());

        if (!StringUtils.hasText(askRequest.approach())) {
            LOGGER.warn("approach cannot be null in ASK request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!StringUtils.hasText(question)) {
            LOGGER.warn("question cannot be null in ASK request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var ragOptions =
                new RAGOptions.Builder()
                        .retrievialMode(askRequest.context().overrides().retrieval_mode().name())
                        .semanticKernelMode(askRequest.context().overrides().semantic_kernel_mode())
                        .semanticRanker(askRequest.context().overrides().semantic_ranker())
                        .semanticCaptions(askRequest.context().overrides().semantic_captions())
                        .excludeCategory(askRequest.context().overrides().exclude_category())
                        .promptTemplate(askRequest.context().overrides().prompt_template())
                        .top(askRequest.context().overrides().top())
                        .build();

        RAGApproach<String, RAGResponse> ragApproach =
                ragApproachFactory.createApproach(askRequest.approach(), RAGType.ASK, ragOptions);

        return ResponseEntity.ok(
                ChatResponse.buildChatResponse(ragApproach.run(question, ragOptions)));
    }
}
