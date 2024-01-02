package com.microsoft.openai.samples.indexer.embeddings;

import java.util.List;

public interface TextEmbeddingsService {
    
    public List<List<Double>> createEmbeddingBatch(List<String> texts);
}
