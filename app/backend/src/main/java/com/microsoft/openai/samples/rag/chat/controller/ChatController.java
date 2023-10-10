package com.microsoft.openai.samples.rag.chat.controller;

import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproachFactory;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.approaches.RAGType;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
public class ChatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatController.class);
    private final RAGApproachFactory<ChatGPTConversation, RAGResponse> ragApproachFactory;

    public ChatController(RAGApproachFactory<ChatGPTConversation, RAGResponse> ragApproachFactory) {
        this.ragApproachFactory = ragApproachFactory;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponse> openAIAsk(@RequestBody ChatAppRequest chatRequest) {
        LOGGER.info("Received request for chat api with approach[{}]", chatRequest.approach());

        if (!StringUtils.hasText(chatRequest.approach())) {
            LOGGER.warn("approach cannot be null in CHAT request");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

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
                .build();

        RAGApproach<ChatGPTConversation, RAGResponse> ragApproach = ragApproachFactory.createApproach(chatRequest.approach(), RAGType.CHAT, ragOptions);


        ChatGPTConversation chatGPTConversation = convertToChatGPT(chatRequest.messages());
        return ResponseEntity.ok(buildChatResponse(ragApproach.run(chatGPTConversation, ragOptions)));

    }

    private ChatGPTConversation convertToChatGPT(List<ResponseMessage> chatHistory) {
        return new ChatGPTConversation(
                chatHistory.stream()
                        .map(historyChat -> {
                            List<ChatGPTMessage> chatGPTMessages = new ArrayList<>();
                            chatGPTMessages.add(new ChatGPTMessage(ChatGPTMessage.ChatRole.fromString(historyChat.role()), historyChat.content()));
                            return chatGPTMessages;
                        })
                        .flatMap(Collection::stream)
                        .toList());
    }

    private ChatResponse buildChatResponse(RAGResponse ragResponse) {
        List<String> dataPoints = ragResponse.getSources().stream()
                .map(source -> source.getSourceName() + ": " + source.getSourceContent())
                .toList();
        String thoughts = "Searched for:<br>" + ragResponse.getQuestion() + "<br><br>Chat:<br>" + ragResponse.getPrompt().replace("\n", "<br>");

        return new ChatResponse(
                List.of(
                        new ResponseChoice(
                                0,
                                new ResponseMessage(
                                        ragResponse.getAnswer(),
                                        "ASSISTANT"
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
