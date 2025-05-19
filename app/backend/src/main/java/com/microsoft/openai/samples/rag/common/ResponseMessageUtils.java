package com.microsoft.openai.samples.rag.common;

import com.azure.ai.openai.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.chat.approaches.AnswerQuestionChatPromptTemplate;
import com.microsoft.openai.samples.rag.controller.*;

import java.util.List;

public class ResponseMessageUtils {

private static final ObjectMapper objectMapper = new ObjectMapper();

public static ChatResponseNEW buildChatResponse(
        ChatGPTConversation questionOrConversation,
        RAGOptions options,
        List<ContentSource> sources,
        AnswerQuestionChatPromptTemplate semanticSearchChat,
        ChatCompletions chatCompletions,
        ChatChoice chatChoice,
        Boolean isStreaming,
        Boolean isDelta) {

    if(isDelta){
        ResponseMessage message = new ResponseMessage(
                chatChoice.getDelta().getContent(),
                null);
        return new ChatResponseNEW(null, null, message);
    }

    try {
        //ResponseThought thought1 = buildPromptToGenerateSearchQueryThought(questionOrConversation, options, chatCompletions);
        //ResponseThought thought2 = buildSearchUsingGeneratedSearchQueryThought(options);
        ResponseThought thought3 = buildSearchResultsThought(sources);
        ResponseThought thought4 = buildPromptToGenerateAnswerThought(semanticSearchChat, chatCompletions);

        // Create dataPoints from sources
        ResponseDataPoint responseDataPoint = createDataPointsFromSources(sources);

        List<ResponseThought> thoughts = List.of(thought3, thought4);
        List<String> followUpQueries = isStreaming ? null:extractFollowUpQueries(chatChoice.getMessage().getContent());;
        ResponseContextNEW context = new ResponseContextNEW(thoughts, responseDataPoint, followUpQueries);

        if(isStreaming){
            ResponseMessage message = new ResponseMessage(
                    chatChoice.getDelta().getContent(),
                    chatChoice.getDelta().getRole().toString());
            return new ChatResponseNEW(null, context, message);
        }
        else {
            ResponseMessage message = new ResponseMessage(
                    chatChoice.getMessage().getContent(),
                    chatChoice.getMessage().getRole().toString());
            return new ChatResponseNEW(message, context, null);
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to build ChatResponseNEW", e);
    }
}

private static ResponseDataPoint createDataPointsFromSources(List<ContentSource> sources) {

    List<String>  textDataPoints = sources.stream()
            .map(source -> source.getSourceName() + ": " + source.getSourceContent())
            .toList();
    return new ResponseDataPoint(null,textDataPoints);
}

private static ResponseThought buildSearchResultsThought(List<ContentSource> sources) throws Exception {
    return new ResponseThought(
        "Search results",
        sources,
        null
    );
}

private static ResponseThought buildPromptToGenerateAnswerThought(AnswerQuestionChatPromptTemplate semanticSearchChat, ChatCompletions chatCompletions) throws Exception {
    List<ResponseMessage> answerPromptMessages = semanticSearchChat.getMessages().stream()
        .map(m -> {
            if (m instanceof ChatRequestUserMessage userMessage) {
                return new ResponseMessage(userMessage.getContent().toString(), userMessage.getRole().toString());
            } else if (m instanceof ChatRequestSystemMessage systemMessage) {
                return new ResponseMessage(systemMessage.getContent().toString(), systemMessage.getRole().toString());
            } else if (m instanceof ChatRequestAssistantMessage assistantMessage) {
                return new ResponseMessage(assistantMessage.getContent().toString(), assistantMessage.getRole().toString());
            } else {
                throw new IllegalArgumentException("Unknown message type: " + m.getClass().getName());
            }
        })
        .toList();

    // Extract model, deployment, and token usage from ChatCompletions
    String model = chatCompletions.getModel();
    var usage = chatCompletions.getUsage();
    int promptTokens = usage != null ? usage.getPromptTokens() : 0;
    int completionTokens = usage != null ? usage.getCompletionTokens() : 0;
    int totalTokens = usage != null ? usage.getTotalTokens() : 0;

    // Build props map
    java.util.Map<String, Object> tokenUsage = new java.util.HashMap<>();
    tokenUsage.put("prompt_tokens", promptTokens);
    tokenUsage.put("completion_tokens", completionTokens);
    tokenUsage.put("reasoning_tokens", 0); // Not available, set to 0
    tokenUsage.put("total_tokens", totalTokens);

    java.util.Map<String, Object> props = new java.util.HashMap<>();
    props.put("model", model);
    props.put("token_usage", tokenUsage);

    return new ResponseThought(
        "Prompt to generate answer",
        answerPromptMessages,
        props
    );
}

public static List<String> extractFollowUpQueries(String input) {
    List<String> results = new java.util.ArrayList<>();
    if (input == null || input.isEmpty()) {
        return results;
    }
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<<(.*?)>>").matcher(input);
    while (matcher.find()) {
        results.add(matcher.group(1).trim());
    }
    return results;
}
}
