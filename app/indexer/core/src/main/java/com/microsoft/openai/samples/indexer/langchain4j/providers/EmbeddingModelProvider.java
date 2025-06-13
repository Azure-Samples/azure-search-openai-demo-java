package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.model.embedding.EmbeddingModel;

public interface EmbeddingModelProvider {


    public EmbeddingModel getEmbeddingModel(PipelineContext ctx) ;
}
