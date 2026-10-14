package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.data.document.DocumentParser;

public interface DocumentParserProvider {


    public DocumentParser getParser(PipelineContext ctx) ;
}
