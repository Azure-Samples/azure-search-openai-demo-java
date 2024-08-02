// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;

import java.util.ArrayList;
import java.util.List;

public class ExtractKeywordsChatTemplate {

    private final List<ChatRequestMessage> conversationHistory = new ArrayList<>();

    private String customPrompt = "";
    private Boolean replacePrompt = false;

    private static final String EXTRACT_KEYWORDS_USER_PROMPT_TEMPLATE =
            """
                    Generate a search query for the below conversation.
                    Do not include cited source filenames and document names e.g info.txt or doc.pdf in the search query terms.
                    Do not include any text inside [] or <<>> in the search query terms.
                    Do not enclose the search query in quotes or double quotes.
                    conversation:
                    %s
                    """;

    public ExtractKeywordsChatTemplate(ChatGPTConversation conversation) {
        if (conversation == null || conversation.getMessages().isEmpty())
            throw new IllegalStateException("conversation cannot be null or empty");

        String chatHistory = ChatGPTUtils.formatAsChatML(conversation.toOpenAIChatMessages());
        // Add user message
        ChatRequestUserMessage chatUserMessage = new ChatRequestUserMessage(EXTRACT_KEYWORDS_USER_PROMPT_TEMPLATE.formatted(chatHistory));

        this.conversationHistory.add(chatUserMessage);

        /**
         * //Add few shoot learning with chat ChatMessage fewShotUser1Message = new
         * ChatMessage(ChatRole.USER); fewShotUser1Message.setContent("What are my health plans?");
         * this.conversationHistory.add(fewShotUser1Message);
         *
         * <p>ChatMessage fewShotAssistant1Message = new ChatMessage(ChatRole.ASSISTANT);
         * fewShotAssistant1Message.setContent("show available health plans");
         * this.conversationHistory.add(fewShotAssistant1Message);
         *
         * <p>ChatMessage fewShotUser2Message = new ChatMessage(ChatRole.USER);
         * fewShotUser2Message.setContent("does my plan cover cardio?");
         * this.conversationHistory.add(fewShotUser2Message);
         *
         * <p>ChatMessage fewShotAssistant2Message = new ChatMessage(ChatRole.ASSISTANT);
         * fewShotAssistant2Message.setContent("Health plan cardio coverage");
         * this.conversationHistory.add(fewShotAssistant2Message);
         */
        // this.conversationHistory.addAll(conversation.toOpenAIChatMessages());
    }

    public List<ChatRequestMessage> getMessages() {
        return this.conversationHistory;
    }
}
