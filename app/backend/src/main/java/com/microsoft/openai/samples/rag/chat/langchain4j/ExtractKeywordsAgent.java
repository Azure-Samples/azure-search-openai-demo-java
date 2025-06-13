// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.chat.langchain4j;

import com.microsoft.openai.samples.rag.common.ChatGPTUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public class ExtractKeywordsAgent {
    interface ExtractKeywordsAssistantAIService{

        @UserMessage("""
Generate a search query for the below conversation.
You have access to Azure AI Search index with 100's of documents
Do not include cited source filenames and document names e.g info.txt or doc.pdf in the search query terms.
Do not include any text inside [] or <<>> in the search query terms.
Do not include any special characters like '+'.
Do not enclose the search query in quotes or double quotes.
If the question is not in English, translate the question to English before generating the search query.
If you cannot generate a search query, return just the number 0.
conversation:
{{conversation}}
""")
        String extract(@V("conversation") String conversation);
    }

    private final String conversation;
    private final ExtractKeywordsAssistantAIService extractKeywordsAssistantAIService;

    public ExtractKeywordsAgent(ChatModel model, List<ChatMessage> conversation) {
        if (conversation == null || conversation.isEmpty())
            throw new IllegalStateException("conversation cannot be null or empty");

        this.conversation = ChatGPTUtils.formatAsChatML(conversation);

        this.extractKeywordsAssistantAIService = AiServices.create(ExtractKeywordsAssistantAIService.class,model);
    }

    public String extractKeywords() {
        return extractKeywordsAssistantAIService.extract(this.conversation);
    }
}
