// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.model;

import java.util.List;

public record ChatAppRequest(
        List<ResponseMessage> messages,
        ChatAppRequestContext context,
        boolean stream,
        String approach) {}
