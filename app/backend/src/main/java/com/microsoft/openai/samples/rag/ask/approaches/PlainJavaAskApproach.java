// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.core.util.IterableStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import com.microsoft.openai.samples.rag.controller.ChatResponse;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.retrieval.FactsRetrieverProvider;
import com.microsoft.openai.samples.rag.retrieval.Retriever;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Use Azure AI Search and Java OpenAI APIs. It first retrieves top documents from search and use
 * them to build a prompt. Then, it uses OpenAI to generate an answer for the user question. Several
 * azure search retrieval options are available: Text, Vector, Hybrid. When Hybrid and Vector
 * are selected an additional call to OpenAI is required to generate embeddings vector for the
 * user question.
 */
@Component
public class PlainJavaAskApproach implements RAGApproach<String, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainJavaAskApproach.class);
    private final OpenAIProxy openAIProxy;
    private final FactsRetrieverProvider factsRetrieverProvider;
    private final ObjectMapper objectMapper;

    public PlainJavaAskApproach(
            FactsRetrieverProvider factsRetrieverProvider,
            OpenAIProxy openAIProxy,
            ObjectMapper objectMapper) {
        this.factsRetrieverProvider = factsRetrieverProvider;
        this.openAIProxy = openAIProxy;
        this.objectMapper = objectMapper;
    }

    /**
     * @param question
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(String question, RAGOptions options) {
        // Get instance of retriever based on the retrieval mode: hybryd, text, vectors.
        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);

        // STEP 1: Retrieve relevant documents using user question as query
        List<ContentSource> sources = factsRetriever.retrieveFromQuestion(question, options);
        LOGGER.info(
                "Total {} sources found in cognitive search for keyword search query[{}]",
                sources.size(),
                question);

        var customPrompt = options.getPromptTemplate();
        var customPromptEmpty =
                (customPrompt == null) || (customPrompt != null && customPrompt.isEmpty());

        // true will replace the default prompt. False will add custom prompt as suffix to the
        // default prompt
        var replacePrompt = !customPromptEmpty && !customPrompt.startsWith("|");
        if (!replacePrompt && !customPromptEmpty) {
            customPrompt = customPrompt.substring(1);
        }

        // STEP 2: Build a prompt using RAG options to see if prompt should be replaced or extended. This is still not grounded with relevant facts
        var answerQuestionChatTemplate =
                new AnswerQuestionPromptTemplate(customPrompt, replacePrompt,sources);

        // STEP 3: Build the chat conversation with grounded messages using the retrieved facts
        var groundedChatMessages = answerQuestionChatTemplate.getMessages(question);
        var chatCompletionsOptions =
                ChatGPTUtils.buildDefaultChatCompletionsOptions(groundedChatMessages);

        // STEP 4: Generate a contextual and content specific answer
        ChatCompletions chatCompletions = openAIProxy.getChatCompletions(chatCompletionsOptions);

        LOGGER.info(
                "Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total"
                        + " Tokens[{}]",
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
     * This is the run streaming version which is implemented as new line delimited json. for more info see https://en.wikipedia.org/wiki/JSON_streaming
     * @param question
     * @param options
     * @param outputStream
     */
    @Override
    public void runStreaming(String question, RAGOptions options, OutputStream outputStream) {
        // Get instance of retriever based on the retrieval mode: hybryd, text, vectors.
        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);
        List<ContentSource> sources = factsRetriever.retrieveFromQuestion(question, options);
        LOGGER.info(
                "Total {} sources found in cognitive search for keyword search query[{}]",
                sources.size(),
                question);

        var customPrompt = options.getPromptTemplate();
        var customPromptEmpty =
                (customPrompt == null) || (customPrompt != null && customPrompt.isEmpty());

        // true will replace the default prompt. False will add custom prompt as suffix to the
        // default prompt
        var replacePrompt = !customPromptEmpty && !customPrompt.startsWith("|");
        if (!replacePrompt && !customPromptEmpty) {
            customPrompt = customPrompt.substring(1);
        }

        var answerQuestionChatTemplate =
                new AnswerQuestionPromptTemplate(customPrompt, replacePrompt,sources);

        var groundedChatMessages = answerQuestionChatTemplate.getMessages(question);
        var chatCompletionsOptions =
                ChatGPTUtils.buildDefaultChatCompletionsOptions(groundedChatMessages);

        IterableStream<ChatCompletions> completions =
                openAIProxy.getChatCompletionsStream(chatCompletionsOptions);
        int index = 0;

        /**
         * For each chat message, generate a response and write it to the output stream provided by the caller.
         */
        for (ChatCompletions completion : completions) {

            LOGGER.info(
                    "Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}],"
                            + " Total Tokens[{}]",
                    completion.getUsage().getPromptTokens(),
                    completion.getUsage().getCompletionTokens(),
                    completion.getUsage().getTotalTokens());

            for (ChatChoice choice : completion.getChoices()) {
                if (choice.getDelta().getContent() == null) {
                    continue;
                }

                RAGResponse ragResponse =
                        new RAGResponse.Builder()
                                .question(question)
                                .prompt(ChatGPTUtils.formatAsChatML(groundedChatMessages))
                                .answer(choice.getMessage().getContent())
                                .sources(sources)
                                .build();

                ChatResponse response;
                if (index == 0) {
                    response = ChatResponse.buildChatResponse(ragResponse);
                } else {
                    response = ChatResponse.buildChatDeltaResponse(index, ragResponse);
                }
                index++;

                try {
                    String value = objectMapper.writeValueAsString(response) + "\n";
                    outputStream.write(value.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
