package com.microsoft.openai.samples.rag.chat.controller;

import java.util.List;

public record ChatAppRequest(
        List<ResponseMessage> messages,
        ChatAppRequestContext context,
        boolean stream,
        String approach
) {
}
