package com.microsoft.openai.samples.rag.history;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends CosmosRepository<ChatHistoryItem, String> {

    @Query(value = "SELECT * FROM c WHERE c.entra_oid = @oid AND c.type = \"session\" ORDER BY c.timestamp DESC offset 0 limit @count")
    List<ChatHistoryItem> findSessions(@Param("oid")String oid, @Param("count")Integer count);

    @Query(value = "SELECT * FROM c WHERE c.entra_oid = @oid AND c.session_id = @sessionId AND c.type = \"message_pair\" ")
    List<ChatHistoryItem> loadSessions(@Param("oid")String oid, @Param("sessionId")String sessionId);

    @Query(value = "SELECT * FROM c WHERE c.entra_oid = @oid AND c.session_id = @sessionId ")
    List<ChatHistoryItem> findChatHistoryItem(@Param("oid")String oid, @Param("sessionId")String sessionId);

}
