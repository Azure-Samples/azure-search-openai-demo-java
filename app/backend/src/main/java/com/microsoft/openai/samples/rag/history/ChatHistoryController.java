package com.microsoft.openai.samples.rag.history;

import com.microsoft.openai.samples.rag.model.ChatAppResponse;
import com.microsoft.openai.samples.rag.security.LoggedUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/chat_history")
@ConditionalOnProperty(name = "app.showChatHistoryCosmos", havingValue = "true")
public class ChatHistoryController {
    @Autowired
    private final ChatHistoryService service;
    private final LoggedUserService loggedUserService;


    public ChatHistoryController(ChatHistoryService service, LoggedUserService loggedUserService) {
        this.service = service;
        this.loggedUserService = loggedUserService;
    }

    // POST /chat_history
    @PostMapping
    public ResponseEntity<?> postChatHistory(@RequestBody ChatHistoryRequest request) {
        String entraOid = loggedUserService.getLoggedUser().entraId();
        String sessionId = request.id();
        List<MessagePair> answers = request.answers();
        String firstQuestion = answers.get(0).getQuestion();
        String title = firstQuestion.length() > 50 ? firstQuestion.substring(0, 50) + "..." : firstQuestion;
        long timestamp = System.currentTimeMillis();

        Session session = new Session();
        session.setId(sessionId);
        session.setVersion("cosmosdb-v2");
        session.setSessionId(sessionId);
        session.setEntraOid(entraOid);
        session.setType("session");
        session.setTitle(title);
        session.setTimestamp(timestamp);

        List<MessagePair> messagePairs = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            MessagePair mp = answers.get(i);
            mp.setId(sessionId + "-" + i);
            mp.setVersion("cosmosdb-v2");
            mp.setSessionId(sessionId);
            mp.setEntraOid(entraOid);
            mp.setType("message_pair");
            messagePairs.add(mp);
        }

        service.saveSessionAndMessages(session, messagePairs);
        return ResponseEntity.status(201).body(Collections.emptyMap());
    }

    // GET /chat_history/sessions
    @GetMapping("/sessions")
    public ResponseEntity<?> getChatHistorySessions(
            @RequestParam(defaultValue = "10") int count) {
        String entraOid = loggedUserService.getLoggedUser().entraId();

        List<ChatHistoryItem> sessions = service.getSessions(entraOid, count);
        return ResponseEntity.ok(Map.of("sessions", sessions));
    }

    // GET /chat_history/sessions/{session_id}
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getChatHistorySession(
            @PathVariable String sessionId) {
        String entraOid = loggedUserService.getLoggedUser().entraId();
        List<ChatHistoryItem> messagePairs = service.getSessionMessages(entraOid, sessionId);

        // Convert to arrays of objects: {question, response}
        List<Object[]> answers = new ArrayList<>();
        for (ChatHistoryItem item : messagePairs) {
            if (item instanceof MessagePair mp) {
                String question = mp.getQuestion();
                ChatAppResponse response = mp.getResponse() ;
                Object[] answer = new Object[2];
                answer[0] = question;
                answer[1] = response;
                answers.add(answer);
            }
        }

        return ResponseEntity.ok(Map.of(
                "id", sessionId,
                "entra_oid", entraOid,
                "answers", answers
        ));
    }

    // DELETE /chat_history/sessions/{session_id}
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteChatHistorySession(
            @PathVariable String sessionId,
            Principal principal) {
        String entraOid = loggedUserService.getLoggedUser().entraId();
        service.deleteSession(entraOid, sessionId);
        return ResponseEntity.noContent().build();
    }
}

