// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import java.util.List;

/**
 * Interface for RAG facts retrievers that can retrieve content from a one shot question or a chat conversation.
 * RAGOptions can be used to specify the retrieval strategy.
 */
public interface Retriever {

    List<ContentSource> retrieveFromQuestion(String question, RAGOptions ragOptions);

    List<ContentSource> retrieveFromConversation(
            ChatGPTConversation conversation, RAGOptions ragOptions);
}
