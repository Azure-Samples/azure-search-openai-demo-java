package com.microsoft.openai.samples.rag.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Overrides {

    @JsonProperty("retrieval_mode")
    private String retrievalMode;
    @JsonProperty("sk_mode")
    private String semantickKernelMode;
    @JsonProperty("semantic_ranker")
    private boolean semanticRanker;
    @JsonProperty("semantic_captions")
    private boolean semanticCaptions;
    @JsonProperty("suggest_followup_questions")
    private boolean suggestFollowupQuestions;
    @JsonProperty("exclude_category")
    private String excludeCategory;
    @JsonProperty("prompt_template")
    private String promptTemplate;
    private Integer top;

    public String getRetrievalMode() {
        return retrievalMode;
    }
    public String getSemantickKernelMode() { return semantickKernelMode; }
    public void setSemanticRanker(boolean semanticRanker) {
        this.semanticRanker = semanticRanker;
    }

    public boolean isSemanticRanker() {
        return semanticRanker;
    }

    public void setSemanticCaptions(boolean semanticCaptions) {
        this.semanticCaptions = semanticCaptions;
    }

    public boolean isSemanticCaptions() {
        return semanticCaptions;
    }

    public boolean isSuggestFollowupQuestions() {
        return suggestFollowupQuestions;
    }

    public void setSuggestFollowupQuestions(boolean suggestFollowupQuestions) {
        this.suggestFollowupQuestions = suggestFollowupQuestions;
    }

    public void setTop(Integer top) {
        this.top = top;
    }

    public Integer getTop() {
        return top;
    }

    public String getExcludeCategory() {
        return excludeCategory;
    }

    public void setExcludeCategory(String excludeCategory) {
        this.excludeCategory = excludeCategory;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

}