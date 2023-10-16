package com.microsoft.openai.samples.rag.approaches;

import com.microsoft.openai.samples.rag.common.ChatGPTConversation;
import reactor.core.publisher.Flux;

import java.io.OutputStream;

public interface RAGApproach<I, O> {

    O run(I questionOrConversation, RAGOptions options);
    void runStreaming(I questionOrConversation, RAGOptions options, OutputStream outputStream);
}
