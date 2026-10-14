package com.microsoft.openai.samples.indexer.langchain4j.providers;

import com.microsoft.openai.samples.indexer.langchain4j.PipelineContext;
import dev.langchain4j.data.document.DocumentTransformer;

public interface DocumentTransformerProvider {


    /**
     * Use a transformer for scenario like:
     * Cleaning: This involves removing unnecessary noise from the Document's text, which can save tokens and reduce distractions.
     * Filtering: to completely exclude particular Documents from the search.
     * Enriching: Additional information can be added to Documents to potentially enhance search results.
     * Summarizing: The Document can be summarized, and its short summary can be stored in the Metadata to be later included in each TextSegment (which we will cover below) to potentially improve the search.
     * If no transformer is provided,transformation step is skipped
     * @param ctx
     * @return
     */
    public DocumentTransformer getTransformer(PipelineContext ctx) ;
}
