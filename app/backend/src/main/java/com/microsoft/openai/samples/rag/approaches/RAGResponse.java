package com.microsoft.openai.samples.rag.approaches;

import java.util.List;

public class RAGResponse {

    private String question;
    private List<ContentSource> sources;
    private String answer;
    private String prompt;

    private RAGResponse(Builder builder) {
        this.question = builder.question;
        this.sources = builder.sources;
        this.answer = builder.answer;
        this.prompt = builder.prompt;
    }


    public static class Builder {
        private String question;
        private List<ContentSource> sources;
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


    public String getQuestion() {
        return question;
    }

    public List<ContentSource> getSources() {
        return sources;
    }


    public String getAnswer() {
        return answer;
    }


    public String getPrompt() {
        return prompt;
    }


}
