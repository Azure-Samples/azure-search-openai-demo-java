// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller;


import com.microsoft.openai.samples.rag.approaches.RAGResponse;
import com.microsoft.openai.samples.rag.common.ChatGPTMessage;

import java.util.Collections;
import java.util.List;

public record ChatResponseNEW(ResponseMessage message, ResponseContextNEW context, ResponseMessage delta) {



}
