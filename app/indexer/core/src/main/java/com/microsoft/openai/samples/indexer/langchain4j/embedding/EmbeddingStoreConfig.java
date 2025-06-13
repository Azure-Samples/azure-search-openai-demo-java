package com.microsoft.openai.samples.indexer.langchain4j.embedding;

import java.util.Map;

public record EmbeddingStoreConfig (String type, Map<String, String> params){}
