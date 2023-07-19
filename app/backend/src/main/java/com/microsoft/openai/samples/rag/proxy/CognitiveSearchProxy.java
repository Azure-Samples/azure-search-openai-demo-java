package com.microsoft.openai.samples.rag.proxy;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This class is a proxy to the Cognitive Search API.
 * It is responsible for:
 * - calling the OpenAI API
 * - handling errors and retry strategy
 * - add monitoring points
 * - add circuit breaker with exponential backoff
 */
@Component
public class CognitiveSearchProxy {

    private SearchClient client;

    public CognitiveSearchProxy(SearchClient searchClient) {

       this.client= searchClient;
    }
    public SearchPagedIterable search(String searchText, SearchOptions searchOptions, Context context){
        return client.search(searchText,searchOptions,context);
    }

}
