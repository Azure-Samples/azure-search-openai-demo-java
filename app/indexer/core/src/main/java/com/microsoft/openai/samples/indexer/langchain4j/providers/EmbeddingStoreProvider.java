package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface EmbeddingStoreProvider<Type> {


    public EmbeddingStore<Type> getEmbeddingStore(PipelineContext ctx) ;
}
