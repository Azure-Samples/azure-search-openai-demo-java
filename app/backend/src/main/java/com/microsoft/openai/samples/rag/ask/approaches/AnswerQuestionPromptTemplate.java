// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.microsoft.openai.samples.rag.approaches.ContentSource;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a prompt for a one shot generate response request.
 * It uses a naive templating system based on simple java string formatting.
 * The prompt is built by injecting the domain specific sources and the user question into the prompt template. Specifically
 *  1. System prompt has instructions for the assistant.
 *  2. Few shot examples are added as user/assistant messages in the messages list (not in the system prompt).
 *  3. User question is grounded with the domain specific sources and added as last message.
 * It doesn't account for the whole conversation history, only the last user message is used. See @AnswerQuestionChatPromptTemplate for an example that uses the whole conversation history.
 * It doesn't account for the maximum token limit of the OpenAI API.
 */
public class AnswerQuestionPromptTemplate {

    private final List<ChatRequestMessage> messages = new ArrayList<>();
    private String customPrompt = "";
    final private String systemMessage;
    private Boolean replacePrompt = false;
    private List<ContentSource> sources;

    private static final String SYSTEM_CHAT_MESSAGE_TEMPLATE =
            """
     You are an intelligent assistant helping Contoso Inc employees with their healthcare plan questions and employee handbook questions.
     Use 'you' to refer to the individual asking the questions even if they ask with 'I'.
     Answer the user question using only the data provided by the user in his message.
     For tabular information return it as an html table. Do not return markdown format.
     Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.
     If you cannot answer say you don't know.
     %s
    """;

    private static final String FEW_SHOT_USER_MESSAGE =
            """
    What is the deductible for the employee plan for a visit to Overlake in Bellevue?'
        Sources:
        info1.txt: deductibles depend on whether you are in-network or out-of-network. In-network deductibles are $500 for employee and $1000 for family. Out-of-network deductibles are $1000 for employee and $2000 for family.
        info2.pdf: Overlake is in-network for the employee plan.
        info3.pdf: Overlake is the name of the area that includes a park and ride near Bellevue.
        info4.pdf: In-network institutions include Overlake, Swedish and others in the region
    """;
    private static final String FEW_SHOT_ASSISTANT_MESSAGE =
            """
    In-network deductibles are $500 for employee and $1000 for family [info1.txt] and Overlake is in-network for the employee plan [info2.pdf][info4.pdf].
    """;

    /**
     * @param conversation conversation history
     * @param sources domain specific sources to be used in the prompt
     * @param customPrompt custom prompt to be injected in the existing promptTemplate or used to
     *     replace it
     * @param replacePrompt if true, the customPrompt will replace the default promptTemplate,
     *     otherwise it will be appended to the default promptTemplate in the predefined section
     */
    private static final String GROUNDED_USER_QUESTION_TEMPLATE =
            """
    %s
    Sources:
    %s
    """;

    public AnswerQuestionPromptTemplate(String customPrompt, Boolean replacePrompt, List<ContentSource> sources) {

        if (replacePrompt && (customPrompt == null || customPrompt.isEmpty()))
            throw new IllegalStateException(
                    "customPrompt cannot be null or empty when replacePrompt is true");

        if (sources == null || sources.isEmpty())
            throw new IllegalStateException("sources cannot be null or empty");

        this.replacePrompt = replacePrompt;
        this.customPrompt = customPrompt == null ? "" : customPrompt;

        if (this.replacePrompt) {
           //custom prompt is used to replace the whole system message
            this.systemMessage = customPrompt;
        } else {
           //custom prompt is used to extend the internal system message
            this.systemMessage = SYSTEM_CHAT_MESSAGE_TEMPLATE.formatted(this.customPrompt);
        }

        this.sources = sources;
        // Add system message
        ChatRequestMessage chatSystemMessage = new ChatRequestSystemMessage(systemMessage);

        this.messages.add(chatSystemMessage);

        // Add few shoot learning as chat user messages
        ChatRequestMessage fewShotUserMessage = new ChatRequestUserMessage(FEW_SHOT_USER_MESSAGE);
        this.messages.add(fewShotUserMessage);

        ChatRequestMessage fewShotAssistantMessage = new ChatRequestAssistantMessage(FEW_SHOT_ASSISTANT_MESSAGE);
        this.messages.add(fewShotAssistantMessage);
    }

    /**
     * Get the grounded messages
     * @param question
     * @return
     */
    public List<ChatRequestMessage> getMessages(String question) {

        if (question == null || question.isEmpty())
            throw new IllegalStateException("question cannot be null");

        StringBuilder sourcesStringBuilder = new StringBuilder();
        // Build sources section
        sources.iterator()
                .forEachRemaining(
                        source ->
                                sourcesStringBuilder
                                        .append(source.getSourceName())
                                        .append(": ")
                                        .append(source.getSourceContent())
                                        .append("\n"));

        // Add user question with retrieved facts
        String groundedUserQuestion =
                GROUNDED_USER_QUESTION_TEMPLATE.formatted(
                        question, sourcesStringBuilder.toString());
        ChatRequestMessage groundedUserMessage = new ChatRequestUserMessage(groundedUserQuestion);
        this.messages.add(groundedUserMessage);

        return this.messages;
    }
}
