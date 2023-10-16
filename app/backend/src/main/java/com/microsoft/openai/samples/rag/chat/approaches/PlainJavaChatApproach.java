package com.microsoft.openai.samples.rag.chat.approaches;

import com.azure.ai.openai.models.ChatCompletions;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.FactsRetrieverProvider;
import com.microsoft.openai.samples.rag.retrieval.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Simple chat-read-retrieve-read java implementation, using the Cognitive Search and OpenAI APIs directly.
 * It uses the ChatGPT API to turn the user question into a good search query.
 * It queries Azure Cognitive Search for search results for that query (optionally using the vector embeddings for that query).
 * It then combines the search results and original user question, and asks ChatGPT API to answer the question based on the sources. It includes the last 4K of message history as well (or however many tokens are allowed by the deployed model).
 */
@Component
public class PlainJavaChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainJavaChatApproach.class);
    private ApplicationContext applicationContext;
    private final OpenAIProxy openAIProxy;
    private final FactsRetrieverProvider factsRetrieverProvider;

    public PlainJavaChatApproach(FactsRetrieverProvider factsRetrieverProvider, OpenAIProxy openAIProxy) {
        this.factsRetrieverProvider = factsRetrieverProvider;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param questionOrConversation
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {

        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromConversation(questionOrConversation, options);
        LOGGER.info("Total {} sources retrieved", sources.size());


        // Replace whole prompt is not supported yet
        var semanticSearchChat = new SemanticSearchChat(questionOrConversation, sources, options.getPromptTemplate(), false, options.isSuggestFollowupQuestions());
        var chatCompletionsOptions = ChatGPTUtils.buildDefaultChatCompletionsOptions(semanticSearchChat.getMessages());

        // STEP 3: Generate a contextual and content specific answer using the search results and chat history
        ChatCompletions chatCompletions = openAIProxy.getChatCompletions(chatCompletionsOptions);

        LOGGER.info("Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total Tokens[{}]",
                chatCompletions.getUsage().getPromptTokens(),
                chatCompletions.getUsage().getCompletionTokens(),
                chatCompletions.getUsage().getTotalTokens());

        return new RAGResponse.Builder()
                .question(ChatGPTUtils.getLastUserQuestion(questionOrConversation.getMessages()))
                .prompt(ChatGPTUtils.formatAsChatML(semanticSearchChat.getMessages()))
                .answer(chatCompletions.getChoices().get(0).getMessage().getContent())
                .sources(sources)
                .build();
    }

    @Override
    public Flux<RAGResponse> runStreaming(ChatGPTConversation questionOrConversation, RAGOptions options) {

        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromConversation(questionOrConversation, options);
        LOGGER.info("Total {} sources retrieved", sources.size());


        // Replace whole prompt is not supported yet
        var semanticSearchChat = new SemanticSearchChat(questionOrConversation, sources, options.getPromptTemplate(), false, options.isSuggestFollowupQuestions());
        var chatCompletionsOptions = ChatGPTUtils.buildDefaultChatCompletionsOptions(semanticSearchChat.getMessages());

        // STEP 3: Generate a contextual and content specific answer using the search results and chat history
        Flux<ChatCompletions> chatCompletions = Flux.fromIterable(openAIProxy.getChatCompletionsStream(chatCompletionsOptions));

        return chatCompletions
                .flatMap(completion -> {
                    if (completion.getUsage() != null) {
                        LOGGER.info("Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total Tokens[{}]",
                                completion.getUsage().getPromptTokens(),
                                completion.getUsage().getCompletionTokens(),
                                completion.getUsage().getTotalTokens());
                    }

                    return Flux.fromIterable(completion.getChoices())
                            .filter(chatChoice -> chatChoice.getDelta().getContent() != null)
                            .map(choice -> {
                                return new RAGResponse.Builder()
                                        .question(ChatGPTUtils.getLastUserQuestion(questionOrConversation.getMessages()))
                                        .prompt(ChatGPTUtils.formatAsChatML(semanticSearchChat.getMessages()))
                                        .answer(choice.getDelta().getContent())
                                        .sources(sources)
                                        .build();
                            });
                });


    }


}
