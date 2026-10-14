package com.microsoft.openai.samples.rag.history;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.showChatHistoryCosmos", havingValue = "true")
public class ChatHistoryService {
    private final CosmosAsyncClient cosmosAsyncClient;
    private final String databaseName;
    private final ChatHistoryRepository repository;
    private final String chatHistoryItemContainerName;

    public ChatHistoryService(CosmosAsyncClient cosmosAsyncClient,
                              @Value("${app.cosmosdb.databaseName}") String databaseName,
                              @Value("${app.cosmosdb.containerName}") String chatHistoryItemContainerName,
                              ChatHistoryRepository repository) {
        this.cosmosAsyncClient = cosmosAsyncClient;
        this.databaseName = databaseName;
        this.repository = repository;
        this.chatHistoryItemContainerName = chatHistoryItemContainerName;
    }

    public void saveSessionAndMessages(Session session, List<MessagePair> messagePairs) {
        CosmosAsyncDatabase database = cosmosAsyncClient.getDatabase(databaseName);
        CosmosAsyncContainer chatHistoryItemContainer = database.getContainer(chatHistoryItemContainerName);

        // Build batch with session and optional message pairs
        var partitionKey = new PartitionKeyBuilder()
            .add(session.getEntraOid())
            .add(session.getSessionId())
            .build();
        CosmosBatch batch = CosmosBatch.createCosmosBatch(partitionKey);
        batch.upsertItemOperation(session);
        if (messagePairs != null && !messagePairs.isEmpty()) {
            for (MessagePair mp : messagePairs) {
                batch.upsertItemOperation(mp);
            }
        }
        // Execute batch for session and messages
        CosmosBatchResponse response = chatHistoryItemContainer.executeCosmosBatch(batch).block();
        if (response == null || !response.isSuccessStatusCode()) {
            throw new RuntimeException("Failed to upsert chat history items: " +
                (response != null ? response.getDiagnostics().toString() : "null response"));
        }
    }

    public List<ChatHistoryItem> getSessions(String entraOid, int count) {
        return repository.findSessions(entraOid, count);

    }

    public List<ChatHistoryItem> getSessionMessages(String entraOid, String sessionId) {
       return repository.loadSessions(entraOid,sessionId);
    }

    public void deleteSession(String entraOid, String sessionId) {
        CosmosAsyncDatabase database = cosmosAsyncClient.getDatabase(databaseName);
        CosmosAsyncContainer chatHistoryItemContainer = database.getContainer(chatHistoryItemContainerName);

        List<ChatHistoryItem> chatHistoryItems = repository.findChatHistoryItem(entraOid,sessionId);

        // Build batch with session and optional message pairs
        var partitionKey = new PartitionKeyBuilder()
                .add(entraOid)
                .add(sessionId)
                .build();
        CosmosBatch batch = CosmosBatch.createCosmosBatch(partitionKey);

        for( ChatHistoryItem item : chatHistoryItems) {
            batch.deleteItemOperation(item.getId());
        }

        // Execute batch for session and messages
        CosmosBatchResponse response = chatHistoryItemContainer.executeCosmosBatch(batch).block();
        if (response == null || !response.isSuccessStatusCode()) {
            throw new RuntimeException("Failed to delete chat history items: " +
                    (response != null ? response.getDiagnostics().toString() : "null response"));
        }

    }
}
