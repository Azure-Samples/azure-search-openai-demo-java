package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.data.document.DocumentSource;

public interface DocumentSourceProvider {


    public DocumentSource getSource(PipelineContext ctx) ;
}
