package com.microsoft.openai.samples.rag.history;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.annotation.Id;

@Container(containerName = "chat-history-v2",
        hierarchicalPartitionKeyPaths= {
        "/entraOid",
        "/sessionId"
})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Session.class, name = "session"),
        @JsonSubTypes.Type(value = MessagePair.class, name = "message_pair")
})
public abstract class ChatHistoryItem {
    @Id
    private String id;
    private String version;

    @JsonProperty("entra_oid")
    private String entraOid;
    @JsonProperty("session_id")
    private String sessionId;

    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEntraOid() {
        return entraOid;
    }

    public void setEntraOid(String entraOid) {
        this.entraOid = entraOid;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

