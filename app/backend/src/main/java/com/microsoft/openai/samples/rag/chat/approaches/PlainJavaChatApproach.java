// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.chat.approaches;

import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.core.util.IterableStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Simple chat-read-retrieve-read java implementation, using the Cognitive Search and OpenAI APIs
 * directly. It first calls OpenAI to generate a search keyword for the chat history and then answer
 * to the last chat question. Several cognitive search retrieval options are available: Text,
 * Vector, Hybrid. When Hybrid and Vector are selected an additional call to OpenAI is required to
 * generate embeddings vector for the chat extracted keywords.
 */
@Component
public class PlainJavaChatApproach implements RAGApproach<ChatGPTConversation, RAGResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainJavaChatApproach.class);
    private final ObjectMapper objectMapper;
    private ApplicationContext applicationContext;
    private final OpenAIProxy openAIProxy;
    private final FactsRetrieverProvider factsRetrieverProvider;

    public PlainJavaChatApproach(
            FactsRetrieverProvider factsRetrieverProvider,
            OpenAIProxy openAIProxy,
            ObjectMapper objectMapper) {
        this.factsRetrieverProvider = factsRetrieverProvider;
        this.openAIProxy = openAIProxy;
        this.objectMapper = objectMapper;
    }

    /**
     * @param questionOrConversation
     * @param options
     * @return
     */
    @Override
    public RAGResponse run(ChatGPTConversation questionOrConversation, RAGOptions options) {
        // Get instance of retriever based on the retrieval mode: hybryd, text, vectors.
        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);

        // STEP 1: Retrieve relevant documents using kewirds extracted from the chat history. An
        // additional call to OpenAI is required to generate keywords.
        List<ContentSource> sources =
                factsRetriever.retrieveFromConversation(questionOrConversation, options);
        LOGGER.info("Total {} sources retrieved", sources.size());

        // STEP 2: Build a grounded prompt using the retrieved documents. RAG options is used to
        // configure additional prompt extension like 'suggesting follow up questions' option.
        var semanticSearchChat =
                new AnswerQuestionChatPromptTemplate(
                        questionOrConversation,
                        sources,
                        options.getPromptTemplate(),
                        false,
                        options.isSuggestFollowupQuestions());
        var chatCompletionsOptions =
                ChatGPTUtils.buildDefaultChatCompletionsOptions(semanticSearchChat.getMessages());

        // STEP 3: Generate a contextual and content specific answer using the search results and
        // chat history
        ChatCompletions chatCompletions = openAIProxy.getChatCompletions(chatCompletionsOptions);

        LOGGER.info(
                "Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}], Total"
                        + " Tokens[{}]",
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
    public void runStreaming(
            ChatGPTConversation questionOrConversation,
            RAGOptions options,
            OutputStream outputStream) {
        Retriever factsRetriever = factsRetrieverProvider.getFactsRetriever(options);
        List<ContentSource> sources =
                factsRetriever.retrieveFromConversation(questionOrConversation, options);
        LOGGER.info("Total {} sources retrieved", sources.size());

        // Replace whole prompt is not supported yet
        var semanticSearchChat =
                new AnswerQuestionChatPromptTemplate(
                        questionOrConversation,
                        sources,
                        options.getPromptTemplate(),
                        false,
                        options.isSuggestFollowupQuestions());
        var chatCompletionsOptions =
                ChatGPTUtils.buildDefaultChatCompletionsOptions(semanticSearchChat.getMessages());

        int index = 0;

        IterableStream<ChatCompletions> completions =
                openAIProxy.getChatCompletionsStream(chatCompletionsOptions);

        for (ChatCompletions completion : completions) {
            if (completion.getUsage() != null) {
                LOGGER.info(
                        "Chat completion generated with Prompt Tokens[{}], Completions Tokens[{}],"
                                + " Total Tokens[{}]",
                        completion.getUsage().getPromptTokens(),
                        completion.getUsage().getCompletionTokens(),
                        completion.getUsage().getTotalTokens());
            }

            List<ChatChoice> choices = completion.getChoices();

            for (ChatChoice choice : choices) {
                if (choice.getDelta().getContent() == null) {
                    continue;
                }

                RAGResponse ragResponse =
                        new RAGResponse.Builder()
                                .question(
                                        ChatGPTUtils.getLastUserQuestion(
                                                questionOrConversation.getMessages()))
                                .prompt(
                                        ChatGPTUtils.formatAsChatML(
                                                semanticSearchChat.getMessages()))
                                .answer(choice.getDelta().getContent())
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
