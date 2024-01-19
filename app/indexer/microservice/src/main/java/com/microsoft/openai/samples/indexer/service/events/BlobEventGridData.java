package com.microsoft.openai.samples.indexer.service.events;

public record BlobEventGridData (
        String contentType,
        String url,
        Integer contentLength
){}
