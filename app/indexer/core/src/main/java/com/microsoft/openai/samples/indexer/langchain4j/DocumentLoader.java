package com.microsoft.openai.samples.indexer.langchain4j;

import dev.langchain4j.data.document.DocumentSource;

public interface DocumentLoader {

    public DocumentSource load(String fileOrUrlPath);
}
