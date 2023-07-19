package com.microsoft.openai.samples.rag.ask.controller;

import com.microsoft.openai.samples.rag.controller.Overrides;

public class AskRequest {

    private String question;
    private String approach;
    private Overrides overrides;
    public void setQuestion(String question) {
        this.question = question;
    }
    public String getQuestion() {
        return question;
    }

    public void setApproach(String approach) {
        this.approach = approach;
    }
    public String getApproach() {
        return approach;
    }

    public void setOverrides(Overrides overrides) {
        this.overrides = overrides;
    }
    public Overrides getOverrides() {
        return overrides;
    }

}