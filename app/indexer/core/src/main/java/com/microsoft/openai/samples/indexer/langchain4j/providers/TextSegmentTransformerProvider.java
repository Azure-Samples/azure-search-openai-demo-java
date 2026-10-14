package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.data.segment.TextSegmentTransformer;

public interface TextSegmentTransformerProvider {



    public TextSegmentTransformer getTransformer(PipelineContext ctx) ;
}
