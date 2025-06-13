package com.microsoft.openai.samples.indexer.langchain4j;

import java.util.HashMap;

/**
 * Carries intermediate data through each step of the ingestion pipeline.
 * Keys:
 *   pathOrUrl        - String
 *   metadata          - IndexingMetadata
 *   inputStream       - InputStream
 *   document          - Document
 *   transformedDoc    - Document
 *   segments          - List<TextSegment>
 *   transformedSegs   - List<TextSegment>
 *   embeddings        - List<Embedding>
 */
public class PipelineContext extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> cls) {
        return (T) super.get(key);
    }
}
