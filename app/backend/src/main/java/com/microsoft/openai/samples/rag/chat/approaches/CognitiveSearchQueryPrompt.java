package com.microsoft.openai.samples.rag.chat.approaches;

public class CognitiveSearchQueryPrompt {
    private String conversationHistory = new String();
    private String question = "";

    final private String promptTemplate = """
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
        if( lastUserMessage.getRole() != ChatGPTMessage.ChatRole.USER)
            throw new IllegalStateException("Cannot create Prompt using question[%s]. Expecting user role and found [%s]".formatted(lastUserMessage.getContent(), lastUserMessage.getRole()));
       this.question = lastUserMessage.getContent();

       // Conversation history is the rest of the messages
        StringBuffer conversationText = new StringBuffer("\n");
        conversation.getMessages().iterator().forEachRemaining(message -> {
            if(message.getRole() == ChatGPTMessage.ChatRole.USER)
                conversationText.append("<|im_start|>user\n")
                                .append(message.getContent())
                                .append("\n<|im_end|>\n");
            else if(message.getRole() == ChatGPTMessage.ChatRole.ASSISTANT)
                conversationText.append("<|im_start|>assistant\n")
                        .append(message.getContent())
                        .append("\n<|im_end|>\n");
        });
        this.conversationHistory = conversationText.toString();
    }

    public  String getFormattedPrompt() {
        if (this.question.isEmpty()  || this.conversationHistory.isEmpty())
            throw new IllegalStateException("Cannot format prompt as question or conversation history are empty ");

        if (this.question == null  || this.question.isEmpty())
            throw new IllegalStateException("question cannot be null or empty. Please use setQuestion() before calling getFormattedPrompt()");

        return promptTemplate.formatted(question, conversationHistory);

    }

}
