package com.microsoft.openai.samples.indexer.langchain4j.parser;

import java.util.List;
import java.util.Map;

public record ParserConfig(String type,
                           List<String> extension,
                           Map<String, String> params
) {}
