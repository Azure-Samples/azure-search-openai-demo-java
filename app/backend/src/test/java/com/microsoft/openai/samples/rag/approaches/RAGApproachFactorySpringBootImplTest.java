package com.microsoft.openai.samples.rag.approaches;

import com.microsoft.openai.samples.rag.ask.approaches.PlainJavaAskApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.JavaSemanticKernelAskApproach;
import com.microsoft.openai.samples.rag.chat.approaches.PlainJavaChatApproach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class RAGApproachFactorySpringBootImplTest {

    @Autowired
    private RAGApproachFactorySpringBootImpl ragApproachFactory;

    @Test
    void testCreateApproachWithRetrieveThenRead() {
        RAGApproach approach = ragApproachFactory.createApproach("jos", RAGType.ASK);
        assertInstanceOf(PlainJavaAskApproach.class, approach);
    }

    @Test
    void testCreateApproachWithChatReadRetrieveRead() {
        RAGApproach approach = ragApproachFactory.createApproach("jsk", RAGType.ASK);
        assertInstanceOf(JavaSemanticKernelAskApproach.class, approach);
    }

    @Test
    void testChatCreateApproachWithChatReadRetrieveRead() {
        RAGApproach approach = ragApproachFactory.createApproach("jos", RAGType.CHAT);
        assertInstanceOf(PlainJavaChatApproach.class, approach);
    }

    @Test
    void testCreateApproachWithInvalidApproachName() {
        assertThrows(IllegalArgumentException.class, () -> ragApproachFactory.createApproach("invalid", RAGType.ASK));
    }

    @Test
    void testCreateApproachWithInvalidCombination() {
        assertThrows(IllegalArgumentException.class, () -> ragApproachFactory.createApproach("rtr", RAGType.CHAT));
    }

}