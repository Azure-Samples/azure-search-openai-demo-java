package com.microsoft.openai.samples.rag.chat.approaches;

import com.azure.ai.openai.models.*;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.CognitiveSearchRetriever;
import com.microsoft.openai.samples.rag.retrieval.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChatReadRetrieveReadApproach implements RAGApproach<ChatGPTConversation, RAGResponse>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatReadRetrieveReadApproach.class);
    private ApplicationContext applicationContext;
    private final OpenAIProxy openAIProxy;
    private final Retriever factsRetriever;

    public ChatReadRetrieveReadApproach(Retriever factsRetriever, OpenAIProxy openAIProxy) {
        this.factsRetriever = factsRetriever;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param questionOrConversation
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {

        Retriever factsRetriever = getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromConversation(questionOrConversation, options);
        LOGGER.info("Total {} sources retrieved", sources.size());


        // Replace whole prompt is not supported yet
        var semanticSearchChat= new SemanticSearchChat(questionOrConversation, sources,options.getPromptTemplate(),false,options.isSuggestFollowupQuestions());
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


    /**
     *
     * @param options rag options containing search types(Cognitive Semantic Search, Cognitive Vector Search, Cognitive Hybrid Search, Semantic Kernel Memory) )
     * @return retriever implementation
     */
    private CognitiveSearchRetriever getFactsRetriever(RAGOptions options) {
        //default to Cognitive Semantic Search for MVP.
        return this.applicationContext.getBean(CognitiveSearchRetriever.class);

    }
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

}
