package com.microsoft.openai.samples.indexer.service.controller;

import com.microsoft.openai.samples.indexer.langchain4j.IndexingMetadata;

import java.util.List;

public record IndexingRequest(String fileOrUrlpath, List<IndexingMetadata> metadata) {
}
