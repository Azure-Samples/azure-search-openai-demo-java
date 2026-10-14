// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.model;

public record ResponseChoice(
        int index, ResponseMessage message, ResponseContext context, ResponseMessage delta) {}
