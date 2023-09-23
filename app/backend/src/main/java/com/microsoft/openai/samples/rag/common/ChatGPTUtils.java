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

        // Due to a potential bug in using JVM 17 and java open SDK 1.0.0-beta.2, we need to provide default for all properties to avoid 404 bad Request on the server
        completionsOptions.setMaxTokens(1024);
        completionsOptions.setTemperature(0.3);
        completionsOptions.setStop(new ArrayList<>(List.of("\n")));
        completionsOptions.setLogitBias(new HashMap<>());
        completionsOptions.setN(1);
        completionsOptions.setStream(false);
        completionsOptions.setUser("search-openai-demo-java");
        completionsOptions.setPresencePenalty(0.0);
        completionsOptions.setFrequencyPenalty(0.0);

        return completionsOptions;
    }

    public static  String formatAsChatML(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        messages.forEach(message -> {
            if(message.getRole() == ChatRole.USER){
                sb.append("<|im_start|>user\n");
            } else if(message.getRole() == ChatRole.ASSISTANT) {
                sb.append("<|im_start|>assistant\n");
            } else {
                sb.append("<|im_start|>system\n");
            }
            sb.append(message.getContent()).append("\n").append("|im_end|").append("\n");
        });
        return sb.toString();
    }

    public static String getLastUserQuestion(List<ChatGPTMessage> messages){
        ChatGPTMessage message = messages.get(messages.size()-1);
        if(message.role() != ChatGPTMessage.ChatRole.USER)
            return message.content();
        return "";
    }
}
