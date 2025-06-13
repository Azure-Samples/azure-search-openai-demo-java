package com.microsoft.openai.samples.rag.history;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(using = ChatHistoryRequestDeserializer.class)
public record ChatHistoryRequest (String id, List<MessagePair> answers){}
