package com.microsoft.openai.samples.indexer.langchain4j;

public class IndexingConfigException extends RuntimeException {
    public IndexingConfigException(String message) {
        super(message);
    }
    public IndexingConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
