package com.microsoft.openai.samples.rag.chat.approaches;

import com.azure.ai.openai.models.ChatMessage;
import com.azure.core.util.ExpandableStringEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ChatGPTConversation {

   private List<ChatGPTMessage> messages = new ArrayList<>();
   private Integer tokenCount = 0;

    public ChatGPTConversation(List<ChatGPTMessage> messages) {
        this.messages = messages;
    }

    public List<ChatGPTMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatGPTMessage> messages) {
        this.messages = messages;
    }

    public List<ChatMessage> toOpenAIChatMessages() {

        return this.messages.stream()
                .map(message ->
                {  ChatMessage chatMessage = new ChatMessage(com.azure.ai.openai.models.ChatRole.fromString(message.getRole().toString()));
                   chatMessage.setContent(message.getContent());
                   return chatMessage;
                })
                .collect(Collectors.toList());
    }




}
