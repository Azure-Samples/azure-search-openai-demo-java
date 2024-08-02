// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.proxy;

import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import org.springframework.stereotype.Component;

/**
 * This class is a proxy to the Azure AI Search API. It is responsible for:
 * - calling the OpenAI API
 * - handling errors and retry strategy
 * - add monitoring points
 * - add circuit breaker with exponential backoff
 */
@Component
public class AzureAISearchProxy {

    private final SearchClient client;

    public AzureAISearchProxy(SearchClient searchClient) {
        this.client = searchClient;
    }

    public SearchPagedIterable search(
            String searchText, SearchOptions searchOptions, Context context) {
        return client.search(searchText, searchOptions, context);
    }
}
