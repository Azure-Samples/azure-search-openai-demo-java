
package com.microsoft.openai.samples.indexer.embeddings;

import java.util.List;

public class EmbeddingBatch {
    private List<String> texts;
    private int tokenLength;

    public EmbeddingBatch(List<String> texts, int tokenLength) {
        this.texts = texts;
        this.tokenLength = tokenLength;
    }

    public List<String> getTexts() {
        return texts;
    }

    public int getTokenLength() {
        return tokenLength;
    }
}