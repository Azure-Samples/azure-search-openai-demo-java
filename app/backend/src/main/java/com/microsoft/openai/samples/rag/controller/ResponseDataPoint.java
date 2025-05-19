// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.controller;

import java.util.List;

public record ResponseDataPoint(List<String> images, List<String> text) {}
