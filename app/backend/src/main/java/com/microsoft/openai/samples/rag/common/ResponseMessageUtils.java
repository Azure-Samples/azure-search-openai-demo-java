package com.microsoft.openai.samples.rag.common;

import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.model.*;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import  dev.langchain4j.rag.content.Content;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Map;

public class ResponseMessageUtils {



    public static ChatAppResponse buildDelta0(RAGOptions options,List<Content> sources, String text){
        ResponseMessage message = new ResponseMessage(
                text,
                ResponseMessage.ChatRole.ASSISTANT);
        ResponseThought thought3 = buildSearchResultsThought(sources);

        ResponseDataPoint responseDataPoint = createDataPointsFromSources(sources);
        List<ResponseThought> thoughts = List.of(thought3);
        ResponseContext context = new ResponseContext(responseDataPoint,thoughts, null);

        return new ChatAppResponse(null,context, message, null, options.getThreadId());
    }

public static ChatAppResponse buildDelta(String text){
    ResponseMessage message = new ResponseMessage(
            text,
            ResponseMessage.ChatRole.ASSISTANT);

    return new ChatAppResponse(null,null, message, null, null);
}
    public static ChatAppResponse buildDeltaComplete(
            List<ChatMessage> conversation,
            RAGOptions options,
            List<Content> sources,
            ChatResponse chatResponse,
            String keywordSearchQuery
    ){

        //ResponseThought thought1 = buildPromptToGenerateSearchQueryThought(?);
        ResponseThought thought2 = buildGeneratedSearchQuery(keywordSearchQuery,options);
        ResponseThought thought3 = buildSearchResultsThought(sources);
        ResponseThought thought4 = buildPromptToGenerateAnswerThought(conversation, chatResponse);

        ResponseDataPoint responseDataPoint = createDataPointsFromSources(sources);

        List<ResponseThought> thoughts = List.of(thought2,thought3, thought4);
        List<String> followUpQueries = extractFollowUpQueries(chatResponse.aiMessage().text());;
        ResponseContext context = new ResponseContext(responseDataPoint,thoughts, followUpQueries);

        ResponseMessage message = new ResponseMessage(
                "",
                ResponseMessage.ChatRole.ASSISTANT);
        return new ChatAppResponse(null, context, message,null,options.getThreadId());

    }

    public static ChatAppResponse buildChatResponse(
            List<ChatMessage> conversation,
            RAGOptions options,
            List<Content> sources,
            ChatResponse chatResponse,
            String keywordSearchQuery
           ){

        //ResponseThought thought1 = buildPromptToGenerateSearchQueryThought(?);
        ResponseThought thought2 = buildGeneratedSearchQuery(keywordSearchQuery,options);
        ResponseThought thought3 = buildSearchResultsThought(sources);
        ResponseThought thought4 = buildPromptToGenerateAnswerThought(conversation, chatResponse);

        ResponseDataPoint responseDataPoint = createDataPointsFromSources(sources);

        List<ResponseThought> thoughts = List.of(thought2,thought3, thought4);
        List<String> followUpQueries = extractFollowUpQueries(chatResponse.aiMessage().text());;
        ResponseContext context = new ResponseContext(responseDataPoint,thoughts, followUpQueries);

        ResponseMessage message = new ResponseMessage(
                chatResponse.aiMessage().text(),
                ResponseMessage.ChatRole.ASSISTANT);
        return new ChatAppResponse(message, context, null,null,options.getThreadId());

    }

    public static ChatAppResponse buildDeltaCompleteResponse(
            List<ChatMessage> conversation,
            RAGOptions options,
            List<Content> sources,
            ChatResponse chatResponse
    ){

        //ResponseThought thought1 = buildPromptToGenerateSearchQueryThought(?);
        //ResponseThought thought2 = buildSearchUsingGeneratedSearchQueryThought(options);
        ResponseThought thought3 = buildSearchResultsThought(sources);
        ResponseThought thought4 = buildPromptToGenerateAnswerThought(conversation, chatResponse);

        ResponseDataPoint responseDataPoint = createDataPointsFromSources(sources);

        List<ResponseThought> thoughts = List.of(thought3, thought4);
        List<String> followUpQueries = extractFollowUpQueries(chatResponse.aiMessage().text());;
        ResponseContext context = new ResponseContext(responseDataPoint,thoughts, followUpQueries);

        return new ChatAppResponse(null, context, null,followUpQueries, options.getThreadId());

    }

    /**
public static ChatAppResponse buildChatResponse(
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
        return new ChatAppResponse(options.getThreadId(),null, null, message);
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
        ResponseContext context = new ResponseContext(thoughts, responseDataPoint, followUpQueries);

        if(isStreaming){
            ResponseMessage message = new ResponseMessage(
                    chatChoice.getDelta().getContent(),
                    chatChoice.getDelta().getRole().toString());
            return new ChatAppResponse(options.getThreadId(), null, context, message);
        }
        else {
            ResponseMessage message = new ResponseMessage(
                    chatChoice.getMessage().getContent(),
                    chatChoice.getMessage().getRole().toString());
            return new ChatAppResponse(options.getThreadId(),message, context, null);
        }
    } catch (Exception e) {
        throw new RuntimeException("Failed to build ChatResponseNEW", e);
    }
}
     */

    private static ResponseThought buildGeneratedSearchQuery(String searchQuery , RAGOptions options) {




        // Build props map
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("use_semantic_caption", options.isSemanticCaptions());
        props.put("top", options.getTop());
        props.put("retrieval_strategy", options.getRetrievalMode()); // Not available, set to 0
        props.put("relevance_score", options.getMinimumSearchScore());


        return new ResponseThought(
                "Generated Search Query and Parameters",
                searchQuery,
                props
        );
    }

private static ResponseDataPoint createDataPointsFromSources(List<Content> sources) {

    List<String>  textDataPoints = sources.stream()
            .map(source -> new StringBuilder().append(source.textSegment().metadata().getString("file_name"))
                    .append(source.textSegment().metadata().getString("page_number") != null
                            ? "#page=" + source.textSegment().metadata().getString("page_number")
                            : "")
                    .append(": ")
                    .append(source.textSegment().text()).toString())
            .toList();
    return new ResponseDataPoint(null,textDataPoints);
}

private static ResponseThought buildSearchResultsThought(List<Content> sources) {

    List<Map<String, String>> sourceMaps = sources.stream()
            .map(source -> {
                Map<String, String> map = new java.util.HashMap<>();
                source.metadata().forEach((k, v) -> map.put(k.toString(), v != null ? v.toString() : null));
                source.textSegment().metadata().toMap().forEach((k, v) -> map.put(k.toString(), v != null ? v.toString() : null));
                map.put("content", source.textSegment().text());
                return map;
            })
            .toList();

    return new ResponseThought(
        "Search results",
            sourceMaps,
        null
    );
}

private static ResponseThought buildPromptToGenerateAnswerThought(List<ChatMessage> messages, ChatResponse chatResponse) {
    List<ResponseMessage> answerPromptMessages = messages.stream()
        .map(m -> {
            if (m instanceof UserMessage userMessage) {
                return new ResponseMessage(userMessage.singleText(), ResponseMessage.ChatRole.USER);
            } else if (m instanceof SystemMessage systemMessage) {
                return new ResponseMessage(systemMessage.text(), ResponseMessage.ChatRole.SYSTEM);
            } else if (m instanceof AiMessage assistantMessage) {
                return new ResponseMessage(assistantMessage.text(), ResponseMessage.ChatRole.ASSISTANT);
            } else {
                throw new IllegalArgumentException("Unknown message type: " + m.getClass().getName());
            }
        })
        .toList();

    // Extract model, deployment, and token usage from ChatCompletions
    String model = chatResponse.modelName();

    // Build props map
    java.util.Map<String, Object> tokenUsage = new java.util.HashMap<>();
    tokenUsage.put("prompt_tokens", chatResponse.tokenUsage().inputTokenCount());
    tokenUsage.put("completion_tokens", chatResponse.tokenUsage().outputTokenCount());
    tokenUsage.put("reasoning_tokens", 0); // Not available, set to 0
    tokenUsage.put("total_tokens", chatResponse.tokenUsage().totalTokenCount());

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
