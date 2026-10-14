package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.data.document.DocumentSplitter;

public interface DocumentSplitterProvider {



    public DocumentSplitter getSplitter(PipelineContext ctx) ;
}
