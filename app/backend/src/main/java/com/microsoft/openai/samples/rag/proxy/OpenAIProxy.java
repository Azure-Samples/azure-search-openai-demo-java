package com.microsoft.openai.samples.rag.proxy;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.core.exception.HttpResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * This class is a proxy to the OpenAI API to simplify cross-cutting concerns management (security, load balancing, monitoring, resiliency).
 * It is responsible for:
 * - calling the OpenAI API
 * - handling errors and retry strategy
 * - load balance requests across open AI instances
 * - add monitoring points
 * - add circuit breaker with exponential backoff
 * <p>
 * It also makes unit testing easy using mockito to provide mock implementation for this bean.
 */
@Component
public class OpenAIProxy {

    private final OpenAIClient client;
    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public OpenAIProxy(OpenAIClient client) {
        this.client = client;
    }

    public Completions getCompletions(CompletionsOptions completionsOptions) {
        Completions completions;
        try {
            completions = client.getCompletions(this.gptChatDeploymentModelId, completionsOptions);
        } catch (HttpResponseException e) {
            throw new ResponseStatusException(e.getResponse().getStatusCode(), "Error calling OpenAI API:" + e.getValue(), e);
        }
        return completions;
    }

    public Completions getCompletions(String prompt) {

        Completions completions;
        try {
            completions = client.getCompletions(this.gptChatDeploymentModelId, prompt);
        } catch (HttpResponseException e) {
            throw new ResponseStatusException(e.getResponse().getStatusCode(), "Error calling OpenAI API:" + e.getMessage(), e);
        }
        return completions;
    }

    public ChatCompletions getChatCompletions(ChatCompletionsOptions chatCompletionsOptions) {
        ChatCompletions chatCompletions;
        try {
            chatCompletions = client.getChatCompletions(this.gptChatDeploymentModelId, chatCompletionsOptions);
        } catch (HttpResponseException e) {
            throw new ResponseStatusException(e.getResponse().getStatusCode(), "Error calling OpenAI API:" + e.getMessage(), e);
        }
        return chatCompletions;
    }

}
