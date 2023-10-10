package com.microsoft.openai.samples.rag.ask.controller;

import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproachFactory;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.approaches.RAGType;
import com.microsoft.openai.samples.rag.chat.controller.ChatAppRequest;
import com.microsoft.openai.samples.rag.chat.controller.ChatResponse;
import com.microsoft.openai.samples.rag.chat.controller.ResponseChoice;
import com.microsoft.openai.samples.rag.chat.controller.ResponseContext;
import com.microsoft.openai.samples.rag.chat.controller.ResponseMessage;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class AskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AskController.class);
    private final RAGApproachFactory<String, RAGResponse> ragApproachFactory;

    AskController(RAGApproachFactory<String, RAGResponse> ragApproachFactory) {
        this.ragApproachFactory = ragApproachFactory;
    }

    @PostMapping("/api/ask")
    public ResponseEntity<ChatResponse> openAIAsk(@RequestBody ChatAppRequest askRequest) {
        String question = askRequest.messages().get(askRequest.messages().size() - 1).content();
        LOGGER.info("Received request for ask api with question [{}] and approach[{}]", question, askRequest.approach());

        if (!StringUtils.hasText(askRequest.approach())) {
            LOGGER.warn("approach cannot be null in ASK request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!StringUtils.hasText(question)) {
            LOGGER.warn("question cannot be null in ASK request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        var ragOptions = new RAGOptions.Builder()
                .retrievialMode(askRequest.context().overrides().retrieval_mode().name())
                .semanticKernelMode(askRequest.context().overrides().semantic_kernel_mode())
                .semanticRanker(askRequest.context().overrides().semantic_ranker())
                .semanticCaptions(askRequest.context().overrides().semantic_captions())
                .excludeCategory(askRequest.context().overrides().exclude_category())
                .promptTemplate(askRequest.context().overrides().prompt_template())
                .top(askRequest.context().overrides().top())
                .build();

        RAGApproach<String, RAGResponse> ragApproach = ragApproachFactory.createApproach(askRequest.approach(), RAGType.ASK, ragOptions);

        return ResponseEntity.ok(buildChatResponse(ragApproach.run(question, ragOptions)));
    }

    private ChatResponse buildChatResponse(RAGResponse ragResponse) {
        List<String> dataPoints = Collections.emptyList();

        if (ragResponse.getSources() != null) {
            dataPoints = ragResponse.getSources().stream()
                    .map(source -> source.getSourceName() + ": " + source.getSourceContent())
                    .toList();
        }

        String thoughts = "Question:<br>" + ragResponse.getQuestion() + "<br><br>Prompt:<br>" + ragResponse.getPrompt().replace("\n", "<br>");

        return new ChatResponse(
                List.of(
                        new ResponseChoice(
                                0,
                                new ResponseMessage(
                                        ragResponse.getAnswer(),
                                        ChatGPTMessage.ChatRole.ASSISTANT.toString()
                                ),
                                new ResponseContext(
                                        thoughts,
                                        dataPoints
                                )
                        )
                )
        );
    }
}