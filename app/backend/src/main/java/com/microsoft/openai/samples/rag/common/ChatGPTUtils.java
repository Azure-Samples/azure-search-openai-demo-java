package com.microsoft.openai.samples.rag.common;

import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.model.ResponseMessage;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

import java.util.ArrayList;
import java.util.List;

public class ChatGPTUtils {

    public static ChatRequestParameters buildDefaultChatParameters(RAGOptions options) {

        return ChatRequestParameters.builder()
                .temperature(options.getTemperature())
                .topP(1.0)
                .maxOutputTokens(1024)
                .build();

    }

    private static final String IM_START_USER = "<|im_start|>user";
    private static final String IM_START_ASSISTANT = "<|im_start|>assistant";
    private static final String IM_START_SYSTEM = "<|im_start|>system";

    public static String formatAsChatML(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        messages.forEach(message -> {
            String content = null;
            if (message instanceof UserMessage userMessage) {
                sb.append(IM_START_USER).append("\n");
                content = userMessage.singleText();
            } else if (message instanceof SystemMessage systemMessage) {
                sb.append(IM_START_SYSTEM).append("\n");
                content = systemMessage.text();
            } else if (message instanceof AiMessage aiMessage) {
                sb.append(IM_START_ASSISTANT).append("\n");
                content = aiMessage.text();
            }

            if (content != null) {
                sb.append(content).append("\n").append("|im_end|").append("\n");
            }
        });
        return sb.toString();
    }


    public static List<ChatMessage> convertToLangchain4J(List<ResponseMessage> chatHistory) {

        List<ChatMessage> chatMessages = new ArrayList<>();
        for (ResponseMessage responseMessage : chatHistory) {
           if( responseMessage.role() == ResponseMessage.ChatRole.USER) {
               chatMessages.add(UserMessage.from(responseMessage.content()));
           } else if (responseMessage.role() == ResponseMessage.ChatRole.ASSISTANT)
                    chatMessages.add(AiMessage.from(responseMessage.content()));
                    else if (responseMessage.role() == ResponseMessage.ChatRole.SYSTEM)
                        chatMessages.add(SystemMessage.from(responseMessage.content()));
        }
        return chatMessages;
    }


}
