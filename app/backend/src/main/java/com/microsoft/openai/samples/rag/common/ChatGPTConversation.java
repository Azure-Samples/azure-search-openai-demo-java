// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.common;

import com.azure.ai.openai.models.ChatMessage;
import java.util.List;

public class ChatGPTConversation {

    private List<ChatGPTMessage> messages;
    private Integer tokenCount = 0;

    public ChatGPTConversation(List<ChatGPTMessage> messages) {
        this.messages = messages;
    }

    public List<ChatMessage> toOpenAIChatMessages() {
        return this.messages.stream()
                .map(
                        message -> {
                            ChatMessage chatMessage =
                                    new ChatMessage(
                                            com.azure.ai.openai.models.ChatRole.fromString(
                                                    message.role().toString()));
                            chatMessage.setContent(message.content());
                            return chatMessage;
                        })
                .toList();
    }

    public List<ChatGPTMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatGPTMessage> messages) {
        this.messages = messages;
    }
}
