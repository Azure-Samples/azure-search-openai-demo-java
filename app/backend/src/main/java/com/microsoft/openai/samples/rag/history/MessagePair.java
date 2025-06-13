package com.microsoft.openai.samples.rag.history;

import com.microsoft.openai.samples.rag.model.ChatAppResponse;

public class MessagePair  extends ChatHistoryItem{
    private String question;
    private ChatAppResponse response;

    public MessagePair() {}

    public MessagePair(String question, ChatAppResponse response) {
        this.question = question;
        this.response = response;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public ChatAppResponse getResponse() {
        return response;
    }

    public void setResponse(ChatAppResponse response) {
        this.response = response;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessagePair that = (MessagePair) o;
        if (question != null ? !question.equals(that.question) : that.question != null) return false;
        return response != null ? response.equals(that.response) : that.response == null;
    }

    @Override
    public int hashCode() {
        int result = question != null ? question.hashCode() : 0;
        result = 31 * result + (response != null ? response.hashCode() : 0);
        return result;
    }
}
