package com.microsoft.openai.samples.rag.approaches;

public interface RAGApproach<I, O> {

    O run(I questionOrConversation, RAGOptions options);





}
