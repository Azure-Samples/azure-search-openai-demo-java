package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.ChatCompletions;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.CognitiveSearchRetriever;
import com.microsoft.openai.samples.rag.retrieval.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Simple retrieve-then-read implementation, using the Cognitive Search and OpenAI APIs directly. It first retrieves
 *     top documents from search, then constructs a prompt with them, and then uses OpenAI to generate a completion
 *     (answer) with that prompt.
 */
@Component
public class RetrieveThenReadApproach implements RAGApproach<String, RAGResponse>, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveThenReadApproach.class);
    private ApplicationContext applicationContext;
    private final OpenAIProxy openAIProxy;
    private final Retriever factsRetriever;

    public RetrieveThenReadApproach(Retriever factsRetriever, OpenAIProxy openAIProxy) {
        this.factsRetriever = factsRetriever;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {
        //TODO exception handling

        Retriever factsRetriever = getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromQuestion(question, options);
        LOGGER.info("Total {} sources found in cognitive search for keyword search query[{}]", sources.size(),
                question);

        var customPrompt = options.getPromptTemplate();
        var customPromptEmpty = (customPrompt == null) || (customPrompt != null && customPrompt.isEmpty());

        //true will replace the default prompt. False will add custom prompt as suffix to the default prompt
        var replacePrompt =  !customPromptEmpty  && !customPrompt.startsWith("|");
        if(!replacePrompt && !customPromptEmpty){
            customPrompt = customPrompt.substring(1);
        }

        var answerQuestionChatTemplate = new AnswerQuestionChatTemplate(customPrompt, replacePrompt);

        var groundedChatMessages = answerQuestionChatTemplate.getMessages(question,sources);
        var chatCompletionsOptions = ChatGPTUtils.buildDefaultChatCompletionsOptions(groundedChatMessages);

        // STEP 3: Generate a contextual and content specific answer using the retrieve facts
        ChatCompletions chatCompletions = openAIProxy.getChatCompletions(chatCompletionsOptions);

        LOGGER.info("Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total Tokens[{}]",
                chatCompletions.getUsage().getPromptTokens(),
                chatCompletions.getUsage().getCompletionTokens(),
                chatCompletions.getUsage().getTotalTokens());

        return new RAGResponse.Builder()
                .question(question)
                .prompt(ChatGPTUtils.formatAsChatML(groundedChatMessages))
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
