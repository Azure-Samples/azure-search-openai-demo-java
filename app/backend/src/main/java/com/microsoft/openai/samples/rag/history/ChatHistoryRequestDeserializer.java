package com.microsoft.openai.samples.rag.history;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.openai.samples.rag.model.ChatAppResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryRequestDeserializer extends JsonDeserializer<ChatHistoryRequest> {
    @Override
    public ChatHistoryRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);
        String id = node.get("id").asText();
        List<MessagePair> answers = new ArrayList<>();
        JsonNode answersNode = node.get("answers");
        if (answersNode != null && answersNode.isArray()) {
            for (JsonNode answerNode : answersNode) {
                String question = answerNode.get(0).asText();
                ChatAppResponse response = mapper.treeToValue(answerNode.get(1), ChatAppResponse.class);
                MessagePair pair = new MessagePair(question, response);
                answers.add(pair);
            }

        }
        return new ChatHistoryRequest(id, answers);
    }
}

