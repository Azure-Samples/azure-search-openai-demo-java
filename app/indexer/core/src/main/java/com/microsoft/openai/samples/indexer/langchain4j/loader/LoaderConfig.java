package com.microsoft.openai.samples.indexer.langchain4j.loader;


import java.util.Map;

public record LoaderConfig(String type,
                           Map<String, String> params
) {}
