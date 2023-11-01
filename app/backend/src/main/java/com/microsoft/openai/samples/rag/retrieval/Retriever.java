// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.retrieval;

import com.microsoft.openai.samples.rag.approaches.ContentSource;
import com.microsoft.openai.samples.rag.approaches.RAGOptions;
import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import java.util.List;

public interface Retriever {

    List<ContentSource> retrieveFromQuestion(String question, RAGOptions ragOptions);

    List<ContentSource> retrieveFromConversation(
            ChatGPTConversation conversation, RAGOptions ragOptions);
}
