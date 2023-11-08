package com.microsoft.openai.samples.rag.common;

import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatGPTUtils {

    public static ChatCompletionsOptions buildDefaultChatCompletionsOptions(List<ChatMessage> messages) {
        ChatCompletionsOptions completionsOptions = new ChatCompletionsOptions(messages);

        completionsOptions.setMaxTokens(1024);
        completionsOptions.setTemperature(0.1);
        completionsOptions.setTopP(1.0);
        //completionsOptions.setStop(new ArrayList<>(List.of("\n")));
        completionsOptions.setLogitBias(new HashMap<>());
        completionsOptions.setN(1);
        completionsOptions.setStream(false);
        completionsOptions.setUser("search-openai-demo-java");
        completionsOptions.setPresencePenalty(0.0);
        completionsOptions.setFrequencyPenalty(0.0);

        return completionsOptions;
    }

    private static final String IM_START_USER = "<|im_start|>user";
    private static final String IM_START_ASSISTANT = "<|im_start|>assistant";
    private static final String IM_START_SYSTEM = "<|im_start|>system";

    public static String formatAsChatML(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        messages.forEach(message -> {
            if (message.getRole() == ChatRole.USER) {
                sb.append(IM_START_USER).append("\n");
            } else if (message.getRole() == ChatRole.ASSISTANT) {
                sb.append(IM_START_ASSISTANT).append("\n");
            } else {
                sb.append(IM_START_SYSTEM).append("\n");
            }
            sb.append(message.getContent()).append("\n").append("|im_end|").append("\n");
        });
        return sb.toString();
    }

    public static List<ChatMessage> parseChatML(String chatML) {
        List<ChatMessage> messages = new ArrayList<>();
        String[] messageTokens = chatML.split("\\|im_end\\|\\n");

        for (String messageToken : messageTokens) {
            String[] lines = messageToken.trim().split("\n");

            if (lines.length >= 2) {
                ChatRole role = ChatRole.SYSTEM;
                if (IM_START_USER.equals(lines[0])) {
                    role = ChatRole.USER;
                } else if (IM_START_ASSISTANT.equals(lines[0])) {
                    role = ChatRole.ASSISTANT;
                }

                StringBuilder content = new StringBuilder();
                for (int i = 1; i < lines.length; ++i) {
                    content.append(lines[i]);
                    if (i < lines.length - 1) {
                        content.append("\n");
                    }
                }

                messages.add(new ChatMessage(role).setContent(content.toString()));
            }
        }

        return messages;
    }

    public static String getLastUserQuestion(List<ChatGPTMessage> messages) {
        List<ChatGPTMessage> userMessages = messages
                .stream()
                .filter(message -> message.role() == ChatGPTMessage.ChatRole.USER)
                .toList();

        if (!userMessages.isEmpty()) {
            return userMessages.get(userMessages.size() - 1).content();
        } else {
            return "";
        }
    }
}
