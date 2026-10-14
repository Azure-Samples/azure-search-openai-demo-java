package com.microsoft.openai.samples.indexer.langchain4j.embedding;

import java.util.Map;

public record EmbeddingModelConfig(String type, Integer dimensions, Map<String,String> params) { }
