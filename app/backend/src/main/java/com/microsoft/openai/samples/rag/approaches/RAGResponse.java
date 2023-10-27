// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

import java.util.List;

public class RAGResponse {

    private final String question;
    private final List<ContentSource> sources;
    private final String sourcesAsText;
    private final String answer;
    private final String prompt;

    private RAGResponse(Builder builder) {
        this.question = builder.question;
        this.sources = builder.sources;
        this.answer = builder.answer;
        this.prompt = builder.prompt;
        this.sourcesAsText = builder.sourcesAsText;
    }

    public String getQuestion() {
        return question;
    }

    public List<ContentSource> getSources() {
        return sources;
    }

    public String getSourcesAsText() {
        return sourcesAsText;
    }

    public String getAnswer() {
        return answer;
    }

    public String getPrompt() {
        return prompt;
    }

    public static class Builder {
        private String question;
        private List<ContentSource> sources;
        private String sourcesAsText;
        private String answer;
        private String prompt;

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder sources(List<ContentSource> sources) {
            this.sources = sources;
            return this;
        }

        public Builder sourcesAsText(String sourcesAsText) {
            this.sourcesAsText = sourcesAsText;
            return this;
        }

        public Builder answer(String answer) {
            this.answer = answer;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public RAGResponse build() {
            return new RAGResponse(this);
        }
    }
}
