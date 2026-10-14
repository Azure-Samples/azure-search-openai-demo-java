// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

public class RAGOptions {

    private RetrievalMode retrievalMode;
    private boolean semanticRanker;
    private boolean semanticCaptions;
    private boolean suggestFollowupQuestions;
    private String excludeCategory;
    private String promptTemplate;
    private Integer top;
    private String threadId;
    private float minimumSearchScore = 0.5f;
    private float minimumRerankerScore = 0.5f;
    private double temperature = 0.3f;

    private RAGOptions() {}

    public RetrievalMode getRetrievalMode() {
        return retrievalMode;
    }

    public boolean isSemanticRanker() {
        return semanticRanker;
    }

    public boolean isSemanticCaptions() {
        return semanticCaptions;
    }

    public String getExcludeCategory() {
        return excludeCategory;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public Integer getTop() {
        return top;
    }

    public boolean isSuggestFollowupQuestions() {
        return suggestFollowupQuestions;
    }

    public String getThreadId() {
        return threadId;
    }

    public float getMinimumSearchScore() { return minimumSearchScore;}

    public float getMinimumRerankerScore() { return minimumRerankerScore;}

    public double getTemperature() { return temperature;
    }

    public static class Builder {
        private RetrievalMode retrievalMode;
        private boolean semanticRanker;
        private boolean semanticCaptions;
        private String excludeCategory;
        private String promptTemplate;
        private Integer top;
        private boolean suggestFollowupQuestions;
        private String threadId;
        private float minimumSearchScore = 0.5f;
        private float minimumRerankerScore = 0.5f;
        private double temperature = 0.3f;

        public Builder retrievialMode(String retrievialMode) {
            this.retrievalMode = RetrievalMode.valueOf(retrievialMode);
            return this;
        }

        public Builder semanticRanker(boolean semanticRanker) {
            this.semanticRanker = semanticRanker;
            return this;
        }

        public Builder semanticCaptions(boolean semanticCaptions) {
            this.semanticCaptions = semanticCaptions;
            return this;
        }

        public Builder suggestFollowupQuestions(boolean suggestFollowupQuestions) {
            this.suggestFollowupQuestions = suggestFollowupQuestions;
            return this;
        }

        public Builder excludeCategory(String excludeCategory) {
            this.excludeCategory = excludeCategory;
            return this;
        }

        public Builder promptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public Builder top(Integer top) {
            this.top = top;
            return this;
        }

        public Builder threadId(String threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder minimumSearchScore(float minimumSearchScore) {
            this.minimumSearchScore = minimumSearchScore;
            return this;
        }

        public Builder minimumRerankerScore(float minimumRerankerScore) {
            this.minimumRerankerScore = minimumRerankerScore;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public RAGOptions build() {
            RAGOptions ragOptions = new RAGOptions();
            ragOptions.retrievalMode = this.retrievalMode;
            ragOptions.semanticRanker = this.semanticRanker;
            ragOptions.semanticCaptions = this.semanticCaptions;
            ragOptions.suggestFollowupQuestions = this.suggestFollowupQuestions;
            ragOptions.excludeCategory = this.excludeCategory;
            ragOptions.promptTemplate = this.promptTemplate;
            ragOptions.top = this.top;
            ragOptions.threadId = this.threadId;
            ragOptions.minimumSearchScore = this.minimumSearchScore;
            ragOptions.minimumRerankerScore = this.minimumRerankerScore;
            ragOptions.temperature = this.temperature;
            return ragOptions;
        }
    }
}
