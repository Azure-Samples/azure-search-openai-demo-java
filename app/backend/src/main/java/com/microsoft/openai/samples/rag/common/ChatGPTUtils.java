package com.microsoft.openai.samples.rag.common;

import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;

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

    public static String formatAsChatML(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        messages.forEach(message -> {
            if (message.getRole() == ChatRole.USER) {
                sb.append("<|im_start|>user\n");
            } else if (message.getRole() == ChatRole.ASSISTANT) {
                sb.append("<|im_start|>assistant\n");
            } else {
                sb.append("<|im_start|>system\n");
            }
            sb.append(message.getContent()).append("\n").append("|im_end|").append("\n");
        });
        return sb.toString();
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
