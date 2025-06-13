package com.microsoft.openai.samples.indexer.langchain4j.splitter;

import com.microsoft.openai.samples.indexer.langchain4j.providers.DocumentSplitterProvider;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;

import java.util.function.Function;

/**
 * Factory class to create instances of DocumentSplitterProvider based on SplitterConfig.
 * Currently supports various types of document splitters from langchain4j and custom HTML splitter.
 */
public class DocumentSplitterProviderFactory {
    private final Function<String, Object> DIResolver;

    public DocumentSplitterProviderFactory(Function<String, Object> diResolver) {
        DIResolver = diResolver;
    }

   public DocumentSplitterProvider create(SplitterConfig config) {
        if (config == null || config.type() == null) {
            throw new IllegalArgumentException("LoaderConfig and its type must not be null");
        }

        switch (config.type()) {
            case "langchain4j-recursive-splitter":
                return ctx -> DocumentSplitters.recursive(config.chunksize(), config.overlap());
            case "langchain4j-split-by-sentence":
                return ctx -> new DocumentBySentenceSplitter(config.chunksize(), config.overlap());
            case "langchain4j-split-by-paragraph":
                return ctx -> new DocumentByParagraphSplitter(config.chunksize(), config.overlap());
            case "html-splitter":
                return ctx -> new PreservingHtmlTableTextSplitter(config.chunksize(), config.sentenceSeachLimit(),config.overlap());
            default:
                throw new IllegalArgumentException("Unsupported splitter type: " + config.type());
        }

    }



}
