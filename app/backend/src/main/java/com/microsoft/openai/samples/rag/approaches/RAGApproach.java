// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

import java.io.OutputStream;

public interface RAGApproach<I, O> {

    O run(I questionOrConversation, RAGOptions options);

    void runStreaming(I questionOrConversation, RAGOptions options, OutputStream outputStream);
}
