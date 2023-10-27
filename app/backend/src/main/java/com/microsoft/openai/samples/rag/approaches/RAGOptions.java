// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

public class RAGOptions {

    private RetrievalMode retrievalMode;
    private SemanticKernelMode semantickKernelMode;
    private boolean semanticRanker;
    private boolean semanticCaptions;
    private boolean suggestFollowupQuestions;
    private String excludeCategory;
    private String promptTemplate;
    private Integer top;

    private RAGOptions() {}

    public RetrievalMode getRetrievalMode() {
        return retrievalMode;
    }

    public SemanticKernelMode getSemantickKernelMode() {
        return semantickKernelMode;
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

    public static class Builder {
        private RetrievalMode retrievalMode;

        private SemanticKernelMode semanticKernelMode;
        private boolean semanticRanker;
        private boolean semanticCaptions;
        private String excludeCategory;
        private String promptTemplate;
        private Integer top;

        private boolean suggestFollowupQuestions;

        public Builder retrievialMode(String retrievialMode) {
            this.retrievalMode = RetrievalMode.valueOf(retrievialMode);
            return this;
        }

        public Builder semanticKernelMode(String semanticKernelMode) {
            this.semanticKernelMode = SemanticKernelMode.valueOf(semanticKernelMode);
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

        public RAGOptions build() {
            RAGOptions ragOptions = new RAGOptions();
            ragOptions.retrievalMode = this.retrievalMode;
            ragOptions.semantickKernelMode = this.semanticKernelMode;
            ragOptions.semanticRanker = this.semanticRanker;
            ragOptions.semanticCaptions = this.semanticCaptions;
            ragOptions.suggestFollowupQuestions = this.suggestFollowupQuestions;
            ragOptions.excludeCategory = this.excludeCategory;
            ragOptions.promptTemplate = this.promptTemplate;
            ragOptions.top = this.top;
            return ragOptions;
        }
    }
}
