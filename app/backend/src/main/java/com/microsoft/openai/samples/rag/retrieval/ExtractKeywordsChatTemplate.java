// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import java.util.ArrayList;
import java.util.List;

public class ExtractKeywordsChatTemplate {

    private final List<ChatMessage> conversationHistory = new ArrayList<>();

    private String customPrompt = "";
    private Boolean replacePrompt = false;

    private static final String USER_CHAT_MESSAGE_TEMPLATE =
            """
    Generate a search query for the below conversation.
    Do not include cited source filenames and document names e.g info.txt or doc.pdf in the search query terms.
    Do not include any text inside [] or <<>> in the search query terms.
    Do not enclose the search query in quotes or double quotes.
    conversation:
    %s
    """;

    /**
     * @param conversation conversation history
     * @param sources domain specific sources to be used in the prompt
     * @param customPrompt custom prompt to be injected in the existing promptTemplate or used to
     *     replace it
     * @param replacePrompt if true, the customPrompt will replace the default promptTemplate,
     *     otherwise it will be appended to the default promptTemplate in the predefined section
     */
    private static final String GROUNDED_USER_QUESTION_TEMPLATE =
            """
    %s
    Sources:
    %s
    """;

    public ExtractKeywordsChatTemplate(ChatGPTConversation conversation) {
        if (conversation == null || conversation.getMessages().isEmpty())
            throw new IllegalStateException("conversation cannot be null or empty");

        String chatHistory = ChatGPTUtils.formatAsChatML(conversation.toOpenAIChatMessages());
        // Add system message
        ChatMessage chatUserMessage = new ChatMessage(ChatRole.USER);
        chatUserMessage.setContent(USER_CHAT_MESSAGE_TEMPLATE.formatted(chatHistory));

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

    public List<ChatMessage> getMessages() {
        return this.conversationHistory;
    }
}
