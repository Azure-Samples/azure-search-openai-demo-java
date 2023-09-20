package com.microsoft.openai.samples.rag.chat.approaches;

import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.microsoft.openai.samples.rag.approaches.ContentSource;

import java.util.ArrayList;
import java.util.List;

public class SemanticSearchChat {

    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private final StringBuilder sources = new StringBuilder();
    private final Boolean followUpQuestions;
    private final String customPrompt;
    private final String systemMessage;
    private Boolean replacePrompt = false;

    private static final String FOLLOW_UP_QUESTIONS_TEMPLATE = """
    Generate three very brief follow-up questions that the user would likely ask next about their healthcare plan and employee handbook. 
    Use double angle brackets to reference the questions, e.g. <<Are there exclusions for prescriptions?>>.
    Try not to repeat questions that have already been asked.
    Only generate questions and do not generate any text before or after the questions, such as 'Next Questions'
    """;
    private static final String SYSTEM_CHAT_MESSAGE_TEMPLATE = """
     Assistant helps the company employees with their healthcare plan questions, and questions about the employee handbook. Be brief in your answers.
     Answer ONLY with the facts listed in the list of sources below. If there isn't enough information below, say you don't know. Do not generate answers that don't use the sources below. If asking a clarifying question to the user would help, ask the question.
     For tabular information return it as an html table. Do not return markdown format.
     Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response. Use square brackets to reference the source, e.g. [info1.txt]. Don't combine sources, list each source separately, e.g. [info1.txt][info2.pdf].
           
    %s
    %s
   Sources:
    %s
    """ ;

    /**
     *
     * @param conversation conversation history
     * @param sources   domain specific sources to be used in the prompt
     * @param customPrompt custom prompt to be injected in the existing promptTemplate or used to replace it
     * @param replacePrompt if true, the customPrompt will replace the default promptTemplate, otherwise it will be appended
     *                      to the default promptTemplate in the predefined section
     */
    public SemanticSearchChat(ChatGPTConversation conversation, List<ContentSource> sources, String customPrompt, Boolean replacePrompt, Boolean followUpQuestions) {
        if (conversation == null  || conversation.getMessages().isEmpty())
            throw new IllegalStateException("conversation cannot be null or empty");
        if (sources == null)
            throw new IllegalStateException("sources cannot be null");

        if(replacePrompt)
            throw new IllegalStateException("replace prompt is not supported yet. please set it to false when custom prompt is provided");

        if(replacePrompt && (customPrompt == null || customPrompt.isEmpty()))
            throw new IllegalStateException("customPrompt cannot be null or empty when replacePrompt is true");

        this.followUpQuestions = followUpQuestions;
        this.replacePrompt = replacePrompt;
        this.customPrompt = customPrompt == null ? "" : customPrompt;

       // Build sources section
        sources.iterator().forEachRemaining(source -> this.sources.append(source.getSourceName()).append(": ").append(source.getSourceContent()).append("\n"));

        this.systemMessage =  SYSTEM_CHAT_MESSAGE_TEMPLATE.formatted(this.followUpQuestions ? FOLLOW_UP_QUESTIONS_TEMPLATE : "",this.customPrompt,this.sources.toString());

        //Add system message
        ChatMessage chatMessage = new ChatMessage(ChatRole.SYSTEM);
        chatMessage.setContent(systemMessage);
        this.conversationHistory.add(chatMessage);

        buildConversationHistory(conversation);
    }

    /**
     *
     * @param conversation conversation history
     * @param sources   domain specific sources to be used in the prompt
     */
    public SemanticSearchChat(ChatGPTConversation conversation, List<ContentSource> sources) {
       this(conversation, sources, null, false,false);
    }

    /**
     *
     * @param conversation conversation history
     * @param sources   domain specific sources to be used in the prompt
     * @param followupQuestions   if true, the followup questions prompt will be injected in the promptTemplate
     */
    public SemanticSearchChat(ChatGPTConversation conversation, List<ContentSource> sources, Boolean followupQuestions) {
        this(conversation, sources, null, false,followupQuestions);
    }

    public  List<ChatMessage> getMessages() {
        return this.conversationHistory;
    }

  private void buildConversationHistory(ChatGPTConversation conversation) {
    // Build conversation history is the rest of the messages
       conversation.getMessages().forEach(message -> {
        if(message.role() == ChatGPTMessage.ChatRole.USER){
            ChatMessage chatMessage = new ChatMessage(ChatRole.USER);
            chatMessage.setContent(message.content());
            this.conversationHistory.add(chatMessage);
        } else if(message.role() == ChatGPTMessage.ChatRole.ASSISTANT) {
            ChatMessage chatMessage = new ChatMessage(ChatRole.ASSISTANT);
            chatMessage.setContent(message.content());
            this.conversationHistory.add(chatMessage);
        }
    });
   }

}
