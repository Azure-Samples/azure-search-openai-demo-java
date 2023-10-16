package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.ChatCompletions;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.FactsRetrieverProvider;
import com.microsoft.openai.samples.rag.retrieval.Retriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.OutputStream;
import java.util.List;

/**
 * Simple retrieve-then-read java implementation, using the Cognitive Search and OpenAI APIs directly. It first retrieves
 * top documents from search, then constructs a prompt with them, and then uses OpenAI to generate a completion
 * (answer) with that prompt.
 */
@Component
public class PlainJavaAskApproach implements RAGApproach<String, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainJavaAskApproach.class);
    private final OpenAIProxy openAIProxy;
    private final FactsRetrieverProvider factsRetrieverProvider;

    public PlainJavaAskApproach(FactsRetrieverProvider factsRetrieverProvider, OpenAIProxy openAIProxy) {
        this.factsRetrieverProvider = factsRetrieverProvider;
        this.openAIProxy = openAIProxy;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {
        //Get instance of retriever based on the retrieval mode: hybryd, text, vectors.
        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromQuestion(question, options);
        LOGGER.info("Total {} sources found in cognitive search for keyword search query[{}]", sources.size(),
                question);

        var customPrompt = options.getPromptTemplate();
        var customPromptEmpty = (customPrompt == null) || (customPrompt != null && customPrompt.isEmpty());

        //true will replace the default prompt. False will add custom prompt as suffix to the default prompt
        var replacePrompt = !customPromptEmpty && !customPrompt.startsWith("|");
        if (!replacePrompt && !customPromptEmpty) {
            customPrompt = customPrompt.substring(1);
        }

        var answerQuestionChatTemplate = new AnswerQuestionChatTemplate(customPrompt, replacePrompt);

        var groundedChatMessages = answerQuestionChatTemplate.getMessages(question, sources);
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

    @Override
    public void runStreaming(String questionOrConversation, RAGOptions options, OutputStream outputStream) {
        throw new UnsupportedOperationException("Streaming not supported for PlainJavaAskApproach");
    }
/*
    @Override
    public Flux<RAGResponse> runStreaming(String question, RAGOptions options) {

        //Get instance of retriever based on the retrieval mode: hybryd, text, vectors.
        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromQuestion(question, options);
        LOGGER.info("Total {} sources found in cognitive search for keyword search query[{}]", sources.size(),
                question);

        var customPrompt = options.getPromptTemplate();
        var customPromptEmpty = (customPrompt == null) || (customPrompt != null && customPrompt.isEmpty());

        //true will replace the default prompt. False will add custom prompt as suffix to the default prompt
        var replacePrompt = !customPromptEmpty && !customPrompt.startsWith("|");
        if (!replacePrompt && !customPromptEmpty) {
            customPrompt = customPrompt.substring(1);
        }

        var answerQuestionChatTemplate = new AnswerQuestionChatTemplate(customPrompt, replacePrompt);

        var groundedChatMessages = answerQuestionChatTemplate.getMessages(question, sources);
        var chatCompletionsOptions = ChatGPTUtils.buildDefaultChatCompletionsOptions(groundedChatMessages);

        Flux<ChatCompletions> completions = Flux.fromIterable(openAIProxy.getChatCompletionsStream(chatCompletionsOptions));
        return completions
                .flatMap(completion -> {
                    LOGGER.info("Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total Tokens[{}]",
                            completion.getUsage().getPromptTokens(),
                            completion.getUsage().getCompletionTokens(),
                            completion.getUsage().getTotalTokens());

                    return Flux.fromIterable(completion.getChoices())
                            .map(choice -> new RAGResponse.Builder()
                                    .question(question)
                                    .prompt(ChatGPTUtils.formatAsChatML(groundedChatMessages))
                                    .answer(choice.getMessage().getContent())
                                    .sources(sources)
                                    .build());
                });
    }

 */
}
