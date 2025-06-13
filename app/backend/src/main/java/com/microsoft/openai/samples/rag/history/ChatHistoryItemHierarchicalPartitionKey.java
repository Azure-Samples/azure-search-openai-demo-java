package com.microsoft.openai.samples.rag.history;



public abstract class ChatHistoryItemHierarchicalPartitionKey {

    private String entraOid;

    private String sessionId;

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


}

