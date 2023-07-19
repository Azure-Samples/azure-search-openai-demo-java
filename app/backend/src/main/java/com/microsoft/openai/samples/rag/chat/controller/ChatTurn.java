package com.microsoft.openai.samples.rag.chat.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatTurn {
    @JsonProperty("user")
    private String userText;
    @JsonProperty("bot")
    private String botText;

    public String getUserText() {
        return userText;
    }

    public void setUserText(String userText) {
        this.userText = userText;
    }

    public String getBotText() {
        return botText;
    }

    public void setBotText(String botText) {
        this.botText = botText;
    }
}
