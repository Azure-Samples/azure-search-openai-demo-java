// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatAppResponse(ResponseMessage message, ResponseContext context, ResponseMessage delta,@JsonProperty("followup_questions") List<String> followupQuestions,@JsonProperty("session_state")String threadId) {




}
