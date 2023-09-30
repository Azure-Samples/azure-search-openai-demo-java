package com.microsoft.openai.samples.rag.ask.approaches.semantickernel;

import com.azure.core.util.Context;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.*;
import com.azure.search.documents.util.SearchPagedIterable;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.CognitiveSearchRetriever;
import com.microsoft.semantickernel.skilldefinition.annotations.DefineSKFunction;
import com.microsoft.semantickernel.skilldefinition.annotations.SKFunctionInputAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;


public class CognitiveSearchPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitiveSearchPlugin.class);
    private final CognitiveSearchProxy cognitiveSearchProxy;
    private final OpenAIProxy openAIProxy;
    private final RAGOptions options;

    public CognitiveSearchPlugin(CognitiveSearchProxy cognitiveSearchProxy, OpenAIProxy openAIProxy , RAGOptions options) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.options = options;
        this.openAIProxy = openAIProxy;
    }

    @DefineSKFunction(name = "Search", description = "Search information relevant to answering a given query")
    public Mono<String> search(
        @SKFunctionInputAttribute(description = "the query to answer")
        String query
    ) {

        CognitiveSearchRetriever retriever = new CognitiveSearchRetriever(this.cognitiveSearchProxy, this.openAIProxy);
        List<ContentSource> sources = retriever.retrieveFromQuestion(query, this.options);

        LOGGER.info("Total {} sources found in cognitive search for keyword search query[{}]", sources.size(),
                query);

        StringBuilder sourcesStringBuilder = new StringBuilder();
        // Build sources section
        sources.iterator().forEachRemaining(source -> sourcesStringBuilder.append(
                                                      source.getSourceName())
                                                      .append(": ")
                                                      .append(source.getSourceContent().replace("\n", ""))
                                                      .append("\n"));
        return Mono.just(sourcesStringBuilder.toString());
    }

}