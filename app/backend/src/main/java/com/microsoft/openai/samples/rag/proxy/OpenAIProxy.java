package com.microsoft.openai.samples.rag.proxy;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.Completions;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This class is a proxy to the OpenAI API to simplify cross-cutting concerns management (security, load balancing, monitoring, resiliency).
 * It is responsible for:
 * - calling the OpenAI API
 * - handling errors and retry strategy
 * - load balance requests across open AI instances
 * - add monitoring points
 * - add circuit breaker with exponential backoff
 *
 * It also makes unit testing easy using mockito to provide mock implementation for this bean.
 */
@Component
public class OpenAIProxy {

    private OpenAIClient client;

    @Value("${openai.gpt.deployment}")
    private String gptDeploymentModelId;

    @Value("${openai.chatgpt.deployment}")
    private String gptChatDeploymentModelId;

    public OpenAIProxy( OpenAIClient client) {

       this.client = client;
    }
    public Completions getCompletions(CompletionsOptions completionsOptions){
        return client.getCompletions(this.gptDeploymentModelId,completionsOptions);
    }

    public Completions getCompletions(String prompt){

        return client.getCompletions(this.gptDeploymentModelId,prompt);
    }
    public ChatCompletions getChatCompletions(ChatCompletionsOptions chatCompletionsOptions){
        return client.getChatCompletions(this.gptChatDeploymentModelId,chatCompletionsOptions);
    }
}
