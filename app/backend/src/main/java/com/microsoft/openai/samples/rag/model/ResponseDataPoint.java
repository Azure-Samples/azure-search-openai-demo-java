// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseDataPoint(List<String> images, List<String> text) {}
