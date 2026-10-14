package com.microsoft.openai.samples.indexer.langchain4j;

import dev.langchain4j.data.document.Document;

import java.util.List;

public interface PagedDocument extends Document {

    Document getPage(int pageNumber);
    List<Document> getAllPages();
}
