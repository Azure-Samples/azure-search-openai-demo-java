package com.microsoft.openai.samples.rag.approaches;

import reactor.core.publisher.Flux;

public interface RAGApproach<I, O> {

    O run(I questionOrConversation, RAGOptions options);
    Flux<O> runStreaming(I questionOrConversation, RAGOptions options);
}
