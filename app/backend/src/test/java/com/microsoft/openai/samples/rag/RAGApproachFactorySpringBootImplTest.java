package com.microsoft.openai.samples.rag;

import com.microsoft.openai.samples.rag.chat.approaches.ChatReadRetrieveReadApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproach;
import com.microsoft.openai.samples.rag.approaches.RAGApproachFactorySpringBootImpl;
import com.microsoft.openai.samples.rag.ask.approaches.RetrieveThenReadApproach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
public class RAGApproachFactorySpringBootImplTest {

    @Autowired
    private RAGApproachFactorySpringBootImpl ragApproachFactory;


    @Test
    public void testCreateApproachWithRetrieveThenRead() {

        RAGApproach approach = ragApproachFactory.createApproach("rtr");
        assertInstanceOf(RetrieveThenReadApproach.class, approach);

    }

    @Test
    public void testCreateApproachWithChatReadRetrieveRead() {

        RAGApproach approach = ragApproachFactory.createApproach("rrr");

        assertInstanceOf(ChatReadRetrieveReadApproach.class, approach);
    }

    @Test
    public void testCreateApproachWithInvalidApproachName() {
        assertThrows(IllegalArgumentException.class, () -> ragApproachFactory.createApproach("invalid"));
    }
}