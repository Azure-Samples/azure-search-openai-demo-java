package com.microsoft.openai.samples.indexer.langchain4j;

public class IndexingProcessingException extends RuntimeException {
    public IndexingProcessingException(String message) {
        super(message);
    }
    public IndexingProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
