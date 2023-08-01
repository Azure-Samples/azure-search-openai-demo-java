package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.*;
import com.azure.core.util.Context;
import com.azure.search.documents.util.SearchPagedIterable;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.chat.approaches.ChatReadRetrieveReadApproach;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Simple retrieve-then-read implementation, using the Cognitive Search and OpenAI APIs directly. It first retrieves
 *     top documents from search, then constructs a prompt with them, and then uses OpenAI to generate a completion
 *     (answer) with that prompt.
 */
@Component
public class RetrieveThenReadApproach implements RAGApproach<String, RAGResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RetrieveThenReadApproach.class);
    private String indexContentFieldName = "content";
    private String indexSourcePageFieldName = "sourcepage";
    private String indexCategoryFieldName = "category";

    private CognitiveSearchProxy cognitiveSearchProxy;
    private OpenAIProxy openAIProxy;
    public RetrieveThenReadApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIProxy openAIProxy) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {
    //TODO exception handling and logging
        SearchPagedIterable searchResults = getCognitiveSearchResults(question, options);

        List<ContentSource> sources = buildSourcesFromSearchResults(options, searchResults);
        logger.info("Total %s sources found in cognitive search for keyword search query[%s]".formatted(sources.size(),question));

        var retrieveThenReadPrompt = new SemanticSearchAskPrompt(sources,question);


        var completionsOptions = buildCompletionsOptions(retrieveThenReadPrompt);


        Completions completionsResults = openAIProxy.getCompletions(completionsOptions);

        logger.info("Completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total Tokens[{}]",
                completionsResults.getUsage().getPromptTokens(),
                completionsResults.getUsage().getCompletionTokens(),
                completionsResults.getUsage().getTotalTokens());


        return new RAGResponse.Builder()
                                .prompt(retrieveThenReadPrompt.getFormattedPrompt())
                                .answer(completionsResults.getChoices().get(0).getText())
                                .sources(sources)
                                .question(question)
                                .build();

    }

    private  CompletionsOptions buildCompletionsOptions(SemanticSearchAskPrompt retrieveThenReadPrompt) {
        CompletionsOptions completionsOptions = new CompletionsOptions(new ArrayList<>( Arrays.asList(retrieveThenReadPrompt.getFormattedPrompt())));

        // Due to a potential bug when using JVM 17 and java openai SDK 1.0.0-beta.2, we need to provide default for all properties to avoid 404 bad Request on the server
        completionsOptions.setMaxTokens(1024);
        completionsOptions.setTemperature(0.3);
        completionsOptions.setStop(new ArrayList<>( Arrays.asList("\n")));
        completionsOptions.setLogitBias(new HashMap<>());
        completionsOptions.setEcho(false);
        completionsOptions.setN(1);
        completionsOptions.setStream(false);
        completionsOptions.setUser( "search-openai-demo-java");
        completionsOptions.setPresencePenalty(0.0);
        completionsOptions.setFrequencyPenalty(0.0);
        completionsOptions.setBestOf(1);


        return completionsOptions;
    }

    private SearchPagedIterable getCognitiveSearchResults(String question, RAGOptions options) {
        var searchOptions = new SearchOptions();

        Optional.ofNullable(options.getTop()).ifPresentOrElse(
                value -> searchOptions.setTop(value),
                () -> searchOptions.setTop(3));
        Optional.ofNullable(options.getExcludeCategory())
                .ifPresentOrElse(
                        value -> searchOptions.setFilter("category ne '%s'".formatted(value.replace("'", "''"))),
                        () -> searchOptions.setFilter(null));

        Optional.ofNullable(options.isSemanticRanker()).ifPresent(isSemanticRanker -> {
           if(isSemanticRanker) {
               searchOptions.setQueryType(QueryType.SEMANTIC);
               searchOptions.setQueryLanguage(QueryLanguage.EN_US);
               searchOptions.setSpeller(QuerySpellerType.LEXICON);
               searchOptions.setSemanticConfigurationName("default");
               searchOptions.setQueryCaption(QueryCaptionType.EXTRACTIVE);
               searchOptions.setQueryCaptionHighlightEnabled(false);
           }
        });

        SearchPagedIterable searchResults = this.cognitiveSearchProxy.search(question, searchOptions, Context.NONE);
        return searchResults;
    }

    private List<ContentSource> buildSourcesFromSearchResults(RAGOptions options, SearchPagedIterable searchResults) {
        List<ContentSource> sources = new ArrayList<ContentSource>();

        searchResults.iterator().forEachRemaining(result ->
        {
           var searchDocument = result.getDocument(SearchDocument.class);

           /**
            If captions is enabled the content source is taken from the captions generated by the semantic ranker.
            Captions are appended sequentially and separated by a dot.
            */
           if(options.isSemanticCaptions()) {
               StringBuffer sourcesContentBuffer = new StringBuffer();

               result.getCaptions().forEach(caption -> {
                   sourcesContentBuffer.append(caption.getText()).append(".");
               });

               sources.add(new ContentSource((String)searchDocument.get("sourcepage"), sourcesContentBuffer.toString()));
           } else {
               //If captions is disabled the content source is taken from the cognitive search index field "content"
               sources.add(new ContentSource((String) searchDocument.get("sourcepage"), (String) searchDocument.get("content")));
           }
        });
        return sources;
    }

    public void setIndexContentFieldName(String indexContentFieldName) {
        this.indexContentFieldName = indexContentFieldName;
    }

    public void setIndexSourcePageFieldName(String indexSourcePageFieldName) {
        this.indexSourcePageFieldName = indexSourcePageFieldName;
    }

    public void setIndexCategoryFieldName(String indexCategoryFieldName) {
        this.indexCategoryFieldName = indexCategoryFieldName;
    }
}
