package com.microsoft.openai.samples.rag.chat.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.openai.samples.rag.controller.Overrides;

import java.util.List;

public class ChatRequest {

    @JsonProperty("history")
    private List<ChatTurn> chatHistory;
    private String approach;
    private Overrides overrides;

    public List<ChatTurn> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatTurn> chatHistory) {
        this.chatHistory = chatHistory;
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

