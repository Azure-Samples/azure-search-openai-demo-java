// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller;

import java.util.List;


public record ResponseContextNEW(List<ResponseThought> thoughts, ResponseDataPoint data_points,List<String> followup_questions) {}

