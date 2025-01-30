// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.Embeddings;
import com.azure.core.util.Context;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.QueryCaption;
import com.azure.search.documents.models.QueryCaptionType;
import com.azure.search.documents.models.QueryType;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SemanticSearchOptions;
import com.azure.search.documents.models.VectorSearchOptions;
import com.azure.search.documents.models.VectorizedQuery;
import com.azure.search.documents.util.SearchPagedIterable;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RetrievalMode;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cognitive Search retriever implementation that uses the Cognitive Search API to retrieve
 * documents from the search index. If retrieval mode is set to vectors or hybrid, it will use
 * OpenAI embedding API to convert the user's query text to an embedding vector The hybrid search is
 * specific to cognitive search feature which fuses the best of text search and vector search.
 */
@Component
public class AzureAISearchRetriever implements Retriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAISearchRetriever.class);
    private final AzureAISearchProxy azureAISearchProxy;
    private final OpenAIProxy openAIProxy;

    public AzureAISearchRetriever(
            AzureAISearchProxy azureAISearchProxy, OpenAIProxy openAIProxy) {
        this.azureAISearchProxy = azureAISearchProxy;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param question
     * @param ragOptions
     * @return the top documents retrieved from the search index based on the user's query text
     */
    @Override
    public List<ContentSource> retrieveFromQuestion(String question, RAGOptions ragOptions) {
        SearchOptions searchOptions = new SearchOptions();
        String searchText = null;

        // If the retrieval mode is set to vectors or hybrid, convert the user's query text to an
        // embeddings vector. The embeddings vector is passed as search options to Azure AI Search index
        if (ragOptions.getRetrievalMode() == RetrievalMode.vectors
                || ragOptions.getRetrievalMode() == RetrievalMode.hybrid) {
            LOGGER.info(
                    "Retrieval mode is set to {}. Retrieving vectors for question [{}]",
                    ragOptions.getRetrievalMode(),
                    question);

            Embeddings response = openAIProxy.getEmbeddings(List.of(question));
            var questionVector =
                    response.getData().get(0).getEmbedding();
            if (ragOptions.getRetrievalMode() == RetrievalMode.vectors) {
                setSearchOptionsForVector(ragOptions, questionVector, searchOptions);
            } else {
                searchText = question;
                setSearchOptionsForHybrid(ragOptions, questionVector, searchOptions);
            }
        } else {
            searchText = question;
            setSearchOptions(ragOptions, searchOptions);
        }

        SearchPagedIterable searchResults =
                azureAISearchProxy.search(searchText, searchOptions, Context.NONE);
        return buildSourcesFromSearchResults(ragOptions, searchResults);
    }

    /**
     * @param conversation
     * @param ragOptions
     * @return facts retrieved from the search index based on GPT optimized search keywords
     * extracted from the chat history
     */
    @Override
    public List<ContentSource> retrieveFromConversation(
            ChatGPTConversation conversation, RAGOptions ragOptions) {

        // STEP 1: Generate an optimized keyword search query based on the chat history and the last
        // question
        var extractKeywordsChatTemplate = new ExtractKeywordsChatTemplate(conversation);
        var chatCompletionsOptions =
                ChatGPTUtils.buildDefaultChatCompletionsOptions(
                        extractKeywordsChatTemplate.getMessages());
        ChatCompletions chatCompletions = openAIProxy.getChatCompletions(chatCompletionsOptions);

        var searchKeywords = chatCompletions.getChoices().get(0).getMessage().getContent();
        LOGGER.info("Search Keywords extracted by Open AI [{}]", searchKeywords);

        // STEP 2: Retrieve relevant documents from the search index with the GPT optimized search
        // keywords
        return retrieveFromQuestion(searchKeywords, ragOptions);
    }

    private List<ContentSource> buildSourcesFromSearchResults(
            RAGOptions options, SearchPagedIterable searchResults) {
        List<ContentSource> sources = new ArrayList<>();

        searchResults
                .iterator()
                .forEachRemaining(
                        result -> {
                            var searchDocument = result.getDocument(SearchDocument.class);

                            /*
                            If captions is enabled the content source is taken from the captions generated by the semantic ranker.
                            Captions are appended sequentially and separated by a dot.
                            */
                            if (options.isSemanticCaptions()) {
                                StringBuilder sourcesContentBuffer = new StringBuilder();

                                result.getSemanticSearch().getQueryCaptions()
                                        .forEach(
                                                caption ->
                                                        sourcesContentBuffer
                                                                .append(caption.getText())
                                                                .append("."));

                                sources.add(
                                        new ContentSource(
                                                (String) searchDocument.get("sourcepage"),
                                                sourcesContentBuffer.toString()));
                            } else {
                                // If captions is disabled the content source is taken from the
                                // cognitive search index field "content"
                                sources.add(
                                        new ContentSource(
                                                (String) searchDocument.get("sourcepage"),
                                                (String) searchDocument.get("content")));
                            }
                        });

        return sources;
    }

    private void setSearchOptionsForHybrid(
            RAGOptions ragOptions, List<Float> questionVector, SearchOptions searchOptions) {
        setSearchOptions(ragOptions, searchOptions);
        setSearchOptionsForVector(ragOptions, questionVector, searchOptions);
    }

    private void setSearchOptionsForVector(
            RAGOptions options, List<Float> questionVector, SearchOptions searchOptions) {

        Optional.ofNullable(options.getTop())
                .ifPresentOrElse(searchOptions::setTop, () -> searchOptions.setTop(3));

        VectorizedQuery query = new VectorizedQuery(questionVector)
                .setKNearestNeighborsCount(options.getTop())
                .setFields("embedding");

        // "embedding" is the field name in the index where the embeddings are stored.
        searchOptions.setVectorSearchOptions(new VectorSearchOptions().setQueries(query));
    }

    private void setSearchOptions(RAGOptions options, SearchOptions searchOptions) {
        Optional.ofNullable(options.getTop())
                .ifPresentOrElse(searchOptions::setTop, () -> searchOptions.setTop(3));
        Optional.ofNullable(options.getExcludeCategory())
                .ifPresentOrElse(
                        value ->
                                searchOptions.setFilter(
                                        "category ne '%s'".formatted(value.replace("'", "''"))),
                        () -> searchOptions.setFilter(null));

        Optional.ofNullable(options.isSemanticRanker())
                .ifPresent(
                        isSemanticRanker -> {
                            if (isSemanticRanker) {
                                searchOptions.setQueryType(QueryType.SEMANTIC);
                                searchOptions.setSemanticSearchOptions(
                                  new SemanticSearchOptions()
                                          .setSemanticConfigurationName("default")
                                          .setQueryCaption(
                                                  new QueryCaption(QueryCaptionType.EXTRACTIVE)
                                                          .setHighlightEnabled(false)
                                          )
                                );
                            }
                        });
    }
}
