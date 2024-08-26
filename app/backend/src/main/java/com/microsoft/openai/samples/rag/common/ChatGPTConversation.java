// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.common;

import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.message.ChatMessageTextContent;

import java.util.List;

/**
 * Simple class to represent the chat history.
 * It also has utility methods to convert the chat history to OpenAI chat messages.
 */
public class ChatGPTConversation {

    private List<ChatGPTMessage> messages;

    public ChatGPTConversation(List<ChatGPTMessage> messages) {
        this.messages = messages;
    }

    public ChatHistory toSKChatHistory() {
        List<ChatMessageTextContent> chatHistory = messages.stream()
                .map(message -> {
                    ChatRole role = ChatRole.fromString(message.role().toString());

                    if (role.equals(ChatRole.USER)) {
                        return ChatMessageTextContent.userMessage(message.content());
                    } else if (role.equals(ChatRole.ASSISTANT)) {
                        return ChatMessageTextContent.assistantMessage(message.content());
                    } else if (role.equals(ChatRole.SYSTEM)) {
                        return ChatMessageTextContent.systemMessage(message.content());
                    }

                    throw new IllegalArgumentException("Unknown chat type");
                })
                .toList();

        return new ChatHistory(chatHistory);
    }

    public List<ChatRequestMessage> toOpenAIChatMessages() {
        return this.messages.stream()
                .map(
                        message -> {

                            ChatRole role = ChatRole.fromString(
                                    message.role().toString());
                            ChatRequestMessage chatMessage = null;

                            if (role.equals(ChatRole.USER)) {
                                chatMessage = new ChatRequestUserMessage(message.content());
                            } else if (role.equals(ChatRole.ASSISTANT)) {
                                chatMessage = new ChatRequestAssistantMessage(message.content());
                            } else if (role.equals(ChatRole.SYSTEM)) {
                                chatMessage = new ChatRequestSystemMessage(message.content());
                            }
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
