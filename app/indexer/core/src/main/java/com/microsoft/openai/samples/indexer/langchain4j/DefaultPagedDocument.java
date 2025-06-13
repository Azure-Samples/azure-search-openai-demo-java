package com.microsoft.openai.samples.indexer.langchain4j;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;

import java.util.List;
import java.util.Objects;

public class DefaultPagedDocument  implements PagedDocument  {

    private List<Document> pages;
    private final Metadata metadata;


    public DefaultPagedDocument(Metadata metadata, List<Document> pages) {
        this.metadata = metadata;
        this.pages = pages;
    }

    @Override
    public Document getPage(int pageNumber) {
        return pages.get(pageNumber);
    }

    @Override
    public List<Document> getAllPages() {
        return pages;
    }

    @Override
    public String text() {
        //Iterate over all pages and concatenate their text
        StringBuilder sb = new StringBuilder();
        for (Document page : pages) {
            sb.append(page.text());
        }
        return sb.toString();
    }

    @Override
    public Metadata metadata() {
        return this.metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DefaultPagedDocument that = (DefaultPagedDocument) o;
        return Objects.equals(pages, that.pages) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pages, metadata);
    }

    @Override
    public String toString() {
        return "DefaultPagedDocument{" +
                "pages=" + pages +
                ", metadata=" + metadata +
                '}';
    }
}
