package com.microsoft.openai.samples.rag.chat.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ChatResponse {

    private String answer;
    @JsonProperty("data_points")
    private List<String> dataPoints;
    private String thoughts;
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    public String getAnswer() {
        return answer;
    }

    public void setDataPoints(List<String> dataPoints) {
        this.dataPoints = dataPoints;
    }
    public List<String> getDataPoints() {
        return dataPoints;
    }

    public void setThoughts(String thoughts) {
        this.thoughts = thoughts;
    }
    public String getThoughts() {
        return thoughts;
    }

}