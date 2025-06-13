// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseThought(String title, Object description, Map<String,Object> props ) {}
