package com.microsoft.openai.samples.rag.chat.approaches;

public class CognitiveSearchQueryPrompt {

    private final String conversationHistory;
    private final String question;

    private static final String PROMPT_TEMPLATE = """
    Below is a history of the conversation so far, and a new question asked by the user that needs to be answered by searching in a knowledge base about employee healthcare plans and the employee handbook.
    Generate a search query based on the conversation and the new question.
    Do not include cited source filenames and document names e.g info.txt or doc.pdf in the search query terms.
    Do not include any text inside [] or <<>> in the search query terms.
    If the question is not in English, translate the question to English before generating the search query.
           
   Chat History:
   {%s}
   
   Question:
   {%s}
   
   Search query:
            """ ;

    public CognitiveSearchQueryPrompt(ChatGPTConversation conversation) {
        if (conversation == null  || conversation.getMessages().isEmpty())
            throw new IllegalStateException("conversation cannot be null or empty");

        //last message is the user question
        ChatGPTMessage lastUserMessage = conversation.getMessages().get(conversation.getMessages().size()-1);
        if(lastUserMessage.role() != ChatGPTMessage.ChatRole.USER)
            throw new IllegalStateException("Cannot create Prompt using question[%s]. Expecting user role and found [%s]".formatted(lastUserMessage.content(), lastUserMessage.role()));
       this.question = lastUserMessage.content();

       // Conversation history is the rest of the messages
        StringBuilder conversationText = new StringBuilder("\n");
        conversation.getMessages().iterator().forEachRemaining(message -> {
            if(message.role() == ChatGPTMessage.ChatRole.USER)
                conversationText.append("<|im_start|>user\n")
                                .append(message.content())
                                .append("\n<|im_end|>\n");
            else if(message.role() == ChatGPTMessage.ChatRole.ASSISTANT)
                conversationText.append("<|im_start|>assistant\n")
                        .append(message.content())
                        .append("\n<|im_end|>\n");
        });
        this.conversationHistory = conversationText.toString();
    }

    public  String getFormattedPrompt() {
        if (this.conversationHistory.isEmpty())
            throw new IllegalStateException("Cannot format prompt as the conversation history is empty");

        if (this.question == null  || this.question.isEmpty())
            throw new IllegalStateException("question cannot be null or empty. Please use setQuestion() before calling getFormattedPrompt()");

        return PROMPT_TEMPLATE.formatted(question, conversationHistory);
    }

}
