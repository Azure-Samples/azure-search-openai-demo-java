package com.microsoft.openai.samples.rag.history;

public class Session extends ChatHistoryItem {
    private String title;
    private Long timestamp;

    public Session() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

