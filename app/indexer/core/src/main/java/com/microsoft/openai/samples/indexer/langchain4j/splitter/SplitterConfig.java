package com.microsoft.openai.samples.indexer.langchain4j.splitter;

public record SplitterConfig(String type,Integer chunksize, Integer overlap, Integer sentenceSeachLimit) {
}
