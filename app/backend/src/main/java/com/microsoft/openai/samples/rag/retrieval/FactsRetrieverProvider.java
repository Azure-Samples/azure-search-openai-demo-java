// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


/**
 *  RAG retriever provider that provides the appropriate retriever based on the options.
 *  Currently only supports Azure AI Search. More useful in the future to support multiple retrieval systems (RedisSearch.Pinecone, etc).
 *  This class is used with RAG implemented with plain openai java client. It's not needed when using semantic kernel memory or vector store
 *  abstractions implementations
 */
@Component
public class FactsRetrieverProvider implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    /**
     * @param options rag options containing search types(Cognitive Semantic Search, Cognitive
     *     Vector Search, Cognitive Hybrid Search) Default is now cognitive search.
     * @return retriever implementation
     */
    public Retriever getFactsRetriever(RAGOptions options) {
        // default to Azure AI search Semantic Search for MVP. More useful in the future to support
        // multiple retrieval systems (RedisSearch.Pinecone, etc)
        switch (options.getRetrievalMode()) {
            case vectors, hybrid, text:
                return this.applicationContext.getBean(AzureAISearchRetriever.class);
            default:
                return this.applicationContext.getBean(AzureAISearchRetriever.class);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
