// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller;

import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;
import java.util.Collections;
import java.util.List;

public record ChatResponse(List<ResponseChoice> choices) {

    public static ChatResponse buildChatResponse(RAGResponse ragResponse) {
        List<String> dataPoints = Collections.emptyList();

        if (ragResponse.getSources() != null) {
            dataPoints =
                    ragResponse.getSources().stream()
                            .map(
                                    source ->
                                            source.getSourceName()
                                                    + ": "
                                                    + source.getSourceContent())
                            .toList();
        }

        String thoughts =
                "Question:<br>"
                        + ragResponse.getQuestion()
                        + "<br><br>Prompt:<br>"
                        + ragResponse.getPrompt().replace("\n", "<br>");

        return new ChatResponse(
                List.of(
                        new ResponseChoice(
                                0,
                                new ResponseMessage(
                                        ragResponse.getAnswer(),
                                        ChatGPTMessage.ChatRole.ASSISTANT.toString()),
                                new ResponseContext(thoughts, dataPoints),
                                new ResponseMessage(
                                        ragResponse.getAnswer(),
                                        ChatGPTMessage.ChatRole.ASSISTANT.toString()))));
    }

    public static ChatResponse buildChatDeltaResponse(Integer index, RAGResponse ragResponse) {
        return new ChatResponse(
                List.of(
                        new ResponseChoice(
                                index,
                                new ResponseMessage(ragResponse.getAnswer(), "ASSISTANT"),
                                null,
                                new ResponseMessage(ragResponse.getAnswer(), "ASSISTANT"))));
    }
}
