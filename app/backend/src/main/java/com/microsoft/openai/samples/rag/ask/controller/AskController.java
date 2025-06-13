// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.controller;

import com.microsoft.openai.samples.rag.model.ChatAppRequest;
import com.microsoft.openai.samples.rag.model.ChatAppResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller providing the api for one shot ask to the RAG model.The APPLICATION_NDJSON_VALUE based API is used for streaming the response.
 * Streaming works only with RAG implementation based on plain java Open AI client sdk. Semantic Kernel doesn't support streaming yet
 */
@RestController
public class AskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AskController.class);


    @PostMapping(value = "/api/ask/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity openAIAskStream(@RequestBody ChatAppRequest askRequest) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Deprecated flow. Please use chat flow");
    }

    @PostMapping("/api/ask")
    public ResponseEntity<ChatAppResponse> openAIAsk(@RequestBody ChatAppRequest askRequest) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Deprecated flow. Please use chat flow");
    }
}
