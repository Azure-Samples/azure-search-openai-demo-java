// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller;

public record ResponseChoice(
        int index, ResponseMessage message, ResponseContext context, ResponseMessage delta) {}
