package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.proxy.CognitiveSearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple retrieve-then-read implementation, using the Cognitive Search and OpenAI APIs directly. It first retrieves
 *     top documents from search, then constructs a prompt with them, and then uses OpenAI to generate a completion
 *     (answer) with that prompt.
 */
@Component
public class RetrieveThenReadApproach implements RAGApproach<String, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveThenReadApproach.class);
    private final CognitiveSearchProxy cognitiveSearchProxy;
    private final OpenAIProxy openAIProxy;

    public RetrieveThenReadApproach(CognitiveSearchProxy cognitiveSearchProxy, OpenAIProxy openAIProxy) {
        this.cognitiveSearchProxy = cognitiveSearchProxy;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param questionOrConversation
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String questionOrConversation, RAGOptions options) {
        //TODO exception handling
        SearchPagedIterable searchResults = getCognitiveSearchResults(questionOrConversation, options);

        List<ContentSource> sources = buildSourcesFromSearchResults(options, searchResults);
        LOGGER.info("Total {} sources found in cognitive search for keyword search query[{}]", sources.size(),
            questionOrConversation);

        var retrieveThenReadPrompt = new SemanticSearchAskPrompt(sources, questionOrConversation);

        var completionsOptions = buildCompletionsOptions(retrieveThenReadPrompt);

        Completions completionsResults = openAIProxy.getCompletions(completionsOptions);

        LOGGER.info("Completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total Tokens[{}]",
                completionsResults.getUsage().getPromptTokens(),
                completionsResults.getUsage().getCompletionTokens(),
                completionsResults.getUsage().getTotalTokens());

        return new RAGResponse.Builder()
                                .prompt(retrieveThenReadPrompt.getFormattedPrompt())
                                .answer(completionsResults.getChoices().get(0).getText())
                                .sources(sources)
                                .question(questionOrConversation)
                                .build();
    }

    private  CompletionsOptions buildCompletionsOptions(SemanticSearchAskPrompt retrieveThenReadPrompt) {
        CompletionsOptions completionsOptions = new CompletionsOptions(new ArrayList<>(Collections.singletonList(retrieveThenReadPrompt.getFormattedPrompt())));
        // Due to a potential bug when using JVM 17 and java openai SDK 1.0.0-beta.2, we need to provide default for all properties to avoid 404 bad Request on the server
        completionsOptions.setStop(List.of("\n"));
        return fillCommonCompletionsOptions(completionsOptions);
    }

    @Override
    public CognitiveSearchProxy getCognitiveSearchProxy() {
        return this.cognitiveSearchProxy;
    }

}
