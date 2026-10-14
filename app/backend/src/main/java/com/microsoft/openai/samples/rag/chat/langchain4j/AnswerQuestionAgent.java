// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.chat.langchain4j;

import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.Map;


/**
 * This class represents langchain4j based assistant who helps to generate a response based on the whole conversation history.
 * The prompt is built by injecting the domain specific sources and the chat history into the prompt template. Specifically
 *  1. System prompt has instructions for the assistant and user question it's grounded with sources.
 *  2. No few shot examples are added.
 *  3. Chat history along with last user question is added to the message list.
 *  4. Follow-up questions generation prompt is added if followUpQuestions is true.
 * It doesn't truncate chat history based on OpenAI model token request limits.
 */
public class AnswerQuestionAgent {

    private MessageWindowChatMemory messageWindowChatMemory ;
    private final StringBuilder sources = new StringBuilder();
    private final Boolean followUpQuestions;
    private final String customPrompt;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    private static final String FOLLOW_UP_QUESTIONS_TEMPLATE =
            """
            Generate 3 very brief follow-up questions that the user would likely ask next.
            Enclose the follow-up questions in double angle brackets. Example:
            <<Are there exclusions for prescriptions?>>
            <<Which pharmacies can be ordered from?>>
            <<What is the limit for over-the-counter medication?>>
            Do not repeat questions that have already been asked.
            Make sure the last question ends with ">>".
            """.stripIndent();

    private static final String SYSTEM_CHAT_MESSAGE_TEMPLATE =
             """
             Assistant helps the company employees with their healthcare plan questions, and questions about the employee handbook. Be brief in your answers.
             Answer ONLY with the facts listed in the list of sources below. If there isn't enough information below, say you don't know. Do not generate answers that don't use the sources below. If asking a clarifying question to the user would help, ask the question.
             For tabular information return it as an html table. Do not return markdown format.
             Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response. Use square brackets to reference the source, e.g. [info1.txt]. Don't combine sources, list each source separately, e.g. [info1.txt][info2.pdf].
             {{customPrompt}}
             {{followUpQuestions}}
             """.stripIndent();

    /**
     * @param previousConversation conversation history
     * @param sources       domain specific sources to be used in the prompt
     * @param customPrompt  custom prompt to be injected in the existing promptTemplate or used to
     *                      replace it
     */
    public AnswerQuestionAgent(
            List<ChatMessage> previousConversation,
            List<Content> sources,
            String customPrompt,
            Boolean followUpQuestions,
            ChatModel chatModel,
            StreamingChatModel streamingChatModel) {


        if (sources == null) throw new IllegalStateException("sources cannot be null");

        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.followUpQuestions = followUpQuestions;
        this.customPrompt = customPrompt == null ? "" : customPrompt;

        // Build sources section
        sources.iterator()
                .forEachRemaining(
                        source ->
                                this.sources
                                        .append(source.textSegment().metadata().getString("file_name"))
                                        .append(source.textSegment().metadata().getString("page_number") != null
                                                ? "#page=" + source.textSegment().metadata().getString("page_number")
                                                : "")
                                        .append(": ")
                                        .append(source.textSegment().text())
                                        .append("\n"));


        PromptTemplate promptTemplate = PromptTemplate.from(SYSTEM_CHAT_MESSAGE_TEMPLATE);

        var systemPrompt = promptTemplate.apply(Map.of(
                "customPrompt", this.customPrompt,
                "followUpQuestions", this.followUpQuestions ? FOLLOW_UP_QUESTIONS_TEMPLATE : ""
        ));

        this.messageWindowChatMemory = MessageWindowChatMemory.builder()
                .id("default")
                .maxMessages(20)
                .build();

        //Adding system prompt to chat history
        this.messageWindowChatMemory.add(SystemMessage.from(systemPrompt.text()));

        //Add previous conversation to the list of messages
        if( previousConversation != null && !previousConversation.isEmpty()) {
            previousConversation.forEach(this.messageWindowChatMemory::add);
        }


    }

public ChatResponse answerQuestion(String question, RAGOptions options) {

    var groundedUserQuestion = getGroundedUserQuestion(question);

    this.messageWindowChatMemory.add(UserMessage.from(groundedUserQuestion));

    ChatRequest request = ChatRequest.builder()
            .messages(this.messageWindowChatMemory.messages())
            //https://github.com/langchain4j/langchain4j/issues/3070?reload=1
            //.parameters(ChatGPTUtils.buildDefaultChatParameters(options))
            .build();

    return chatModel.chat(request);
    }

public void answerQuestionStream(String question, RAGOptions options, StreamingChatResponseHandler handler) {

    var groundedUserQuestion = getGroundedUserQuestion(question);

    this.messageWindowChatMemory.add(UserMessage.from(groundedUserQuestion));

    ChatRequest request = ChatRequest.builder()
            .messages(this.messageWindowChatMemory.messages())
            //https://github.com/langchain4j/langchain4j/issues/3070?reload=1
            //.parameters(ChatGPTUtils.buildDefaultChatParameters(options))
            .build();

      streamingChatModel.chat(request,handler);
}

    public List<ChatMessage> getMessages() {
        return this.messageWindowChatMemory.messages();
        }

    private String getGroundedUserQuestion(String question) {
        String userQuestionPrompt = """
                {{question}}
                 Sources:
                 {{sources}}
                """.stripIndent();

        return PromptTemplate
                .from(userQuestionPrompt)
                .apply(Map.of(
                        "question", question,
                        "sources", this.sources.toString()))
                .text();
    }
}


