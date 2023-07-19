package com.microsoft.openai.samples.rag.approaches;

import java.util.Optional;

public interface RAGApproach<INPUT,OUTPUT> {

    public OUTPUT run(INPUT questionOrCoversation, RAGOptions options);

}
