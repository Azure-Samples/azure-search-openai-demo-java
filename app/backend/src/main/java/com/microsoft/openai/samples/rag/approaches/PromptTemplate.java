// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

public interface PromptTemplate {

    String getPrompt();

    void setVariables();
}
