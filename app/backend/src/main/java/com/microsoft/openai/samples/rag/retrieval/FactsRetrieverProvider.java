// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class FactsRetrieverProvider implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    /**
     * @param options rag options containing search types(Cognitive Semantic Search, Cognitive
     *     Vector Search, Cognitive Hybrid Search) Default is now cognitive search.
     * @return retriever implementation
     */
    public Retriever getFactsRetriever(RAGOptions options) {
        // default to Cognitive Semantic Search for MVP. More useful in the future to support
        // multiple retrieval systems (RedisSearch.Pinecone, etc)
        switch (options.getRetrievalMode()) {
            case vectors, hybrid, text:
                return this.applicationContext.getBean(CognitiveSearchRetriever.class);
            default:
                return this.applicationContext.getBean(CognitiveSearchRetriever.class);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
