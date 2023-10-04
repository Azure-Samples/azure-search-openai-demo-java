package com.microsoft.openai.samples.rag.ask.approaches;

import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.microsoft.openai.samples.rag.approaches.ContentSource;
import java.util.ArrayList;
import java.util.List;

public class AnswerQuestionChatTemplate {

    private final List<ChatMessage> conversationHistory = new ArrayList<>();

    private String customPrompt = "";
    private String systemMessage;
    private Boolean replacePrompt = false;

    private static final String SYSTEM_CHAT_MESSAGE_TEMPLATE = """
     You are an intelligent assistant helping Contoso Inc employees with their healthcare plan questions and employee handbook questions.
     Use 'you' to refer to the individual asking the questions even if they ask with 'I'.
     Answer the user question using only the data provided by the user in his message.
     For tabular information return it as an html table. Do not return markdown format.
     Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.
     If you cannot answer say you don't know.   
     %s
    """ ;

    private static final String FEW_SHOT_USER_MESSAGE = """
    What is the deductible for the employee plan for a visit to Overlake in Bellevue?'
        Sources:
        info1.txt: deductibles depend on whether you are in-network or out-of-network. In-network deductibles are $500 for employee and $1000 for family. Out-of-network deductibles are $1000 for employee and $2000 for family.
        info2.pdf: Overlake is in-network for the employee plan.
        info3.pdf: Overlake is the name of the area that includes a park and ride near Bellevue.
        info4.pdf: In-network institutions include Overlake, Swedish and others in the region
    """;
    private static final String FEW_SHOT_ASSISTANT_MESSAGE = """
    In-network deductibles are $500 for employee and $1000 for family [info1.txt] and Overlake is in-network for the employee plan [info2.pdf][info4.pdf].
    """;

    /**
     *
     * @param conversation conversation history
     * @param sources   domain specific sources to be used in the prompt
     * @param customPrompt custom prompt to be injected in the existing promptTemplate or used to replace it
     * @param replacePrompt if true, the customPrompt will replace the default promptTemplate, otherwise it will be appended
     *                      to the default promptTemplate in the predefined section
     */

    private static final String GROUNDED_USER_QUESTION_TEMPLATE = """
    %s
    Sources:
    %s
    """;
    public AnswerQuestionChatTemplate( String customPrompt, Boolean replacePrompt) {

        if(replacePrompt && (customPrompt == null || customPrompt.isEmpty()))
            throw new IllegalStateException("customPrompt cannot be null or empty when replacePrompt is true");

        this.replacePrompt = replacePrompt;
        this.customPrompt = customPrompt == null ? "" : customPrompt;


        if(this.replacePrompt){
            this.systemMessage = customPrompt;
        } else {
            this.systemMessage =  SYSTEM_CHAT_MESSAGE_TEMPLATE.formatted(this.customPrompt);
        }

        //Add system message
        ChatMessage chatSystemMessage = new ChatMessage(ChatRole.SYSTEM);
        chatSystemMessage.setContent(systemMessage);

        this.conversationHistory.add(chatSystemMessage);

        //Add few shoot learning with chat
        ChatMessage fewShotUserMessage = new ChatMessage(ChatRole.USER);
        fewShotUserMessage.setContent(FEW_SHOT_USER_MESSAGE);
        this.conversationHistory.add(fewShotUserMessage);

        ChatMessage fewShotAssistantMessage = new ChatMessage(ChatRole.ASSISTANT);
        fewShotAssistantMessage.setContent(FEW_SHOT_ASSISTANT_MESSAGE);
        this.conversationHistory.add(fewShotAssistantMessage);
    }


    public  List<ChatMessage> getMessages(String question,List<ContentSource> sources ) {
        if (sources == null  || sources.isEmpty())
            throw new IllegalStateException("sources cannot be null or empty");
        if (question == null || question.isEmpty())
            throw new IllegalStateException("question cannot be null");

        StringBuilder sourcesStringBuilder = new StringBuilder();
        // Build sources section
        sources.iterator().forEachRemaining(source -> sourcesStringBuilder.append(source.getSourceName()).append(": ").append(source.getSourceContent()).append("\n"));

        //Add user question with retrieved facts
        String groundedUserQuestion = GROUNDED_USER_QUESTION_TEMPLATE.formatted(question,sourcesStringBuilder.toString());
        ChatMessage groundedUserMessage = new ChatMessage(ChatRole.USER);
        groundedUserMessage.setContent(groundedUserQuestion);
        this.conversationHistory.add(groundedUserMessage);

        return this.conversationHistory;
    }


}
