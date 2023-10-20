// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

public interface RAGApproachFactory<I, O> {

    RAGApproach<I, O> createApproach(String approachName, RAGType ragType, RAGOptions options);
}
