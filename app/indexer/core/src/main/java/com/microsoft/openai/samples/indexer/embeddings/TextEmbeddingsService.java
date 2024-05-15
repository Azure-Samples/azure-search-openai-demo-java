package com.microsoft.openai.samples.indexer.embeddings;

import java.util.List;

public interface TextEmbeddingsService {
    
    public List<List<Float>> createEmbeddingBatch(List<String> texts);
}
