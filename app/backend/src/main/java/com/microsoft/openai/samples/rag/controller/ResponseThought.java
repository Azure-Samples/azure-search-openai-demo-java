// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller;

import java.util.List;
import java.util.Map;

public record ResponseThought(String title, Object description, Map<String,Object> props ) {}
