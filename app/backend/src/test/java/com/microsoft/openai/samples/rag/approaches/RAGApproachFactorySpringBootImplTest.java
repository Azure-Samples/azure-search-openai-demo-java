// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.approaches;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.core.credential.TokenCredential;
import com.azure.search.documents.SearchAsyncClient;
import com.microsoft.openai.samples.rag.ask.approaches.PlainJavaAskApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.JavaSemanticKernelChainsApproach;
import com.microsoft.openai.samples.rag.ask.approaches.semantickernel.JavaSemanticKernelWithVectorStoreApproach;
import com.microsoft.openai.samples.rag.chat.approaches.PlainJavaChatApproach;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class RAGApproachFactorySpringBootImplTest {

    @MockBean private AzureAISearchProxy azureAISearchProxyMock;

    @MockBean private TokenCredential tokenCredential;

    @MockBean private SearchAsyncClient searchAsyncClient;
    @MockBean private OpenAIAsyncClient openAIAsyncClient;

    @Autowired private RAGApproachFactorySpringBootImpl ragApproachFactory;

    @Test
    void testCreateApproachWithJavaPlain() {
        RAGApproach approach = ragApproachFactory.createApproach("jos", RAGType.ASK, null);
        assertInstanceOf(PlainJavaAskApproach.class, approach);
    }

    @Test
    void testCreateApproachWithJavaSemanticKernelMemory() {
        RAGApproach approach = ragApproachFactory.createApproach("jsk", RAGType.ASK, null);
        assertInstanceOf(JavaSemanticKernelWithVectorStoreApproach.class, approach);
    }

    @Test
    void testCreateApproachWithJavaSemanticKernelChain() {
        var ragOptions = new RAGOptions.Builder().semanticKernelMode("chains").build();
        RAGApproach approach = ragApproachFactory.createApproach("jskp", RAGType.ASK, ragOptions);
        assertInstanceOf(JavaSemanticKernelChainsApproach.class, approach);
    }

     @Test
    void testCreateApproachWithJavaSemanticKernelPlanner() {
    var ragOptions = new RAGOptions.Builder().semanticKernelMode("planner").build();
    assertThrows(IllegalArgumentException.class, () -> {
        RAGApproach approach = ragApproachFactory.createApproach("jskp", RAGType.ASK, ragOptions);
    });
}
    @Test
    void testChatCreateApproachWithChat() {
        RAGApproach approach = ragApproachFactory.createApproach("jos", RAGType.CHAT, null);
        assertInstanceOf(PlainJavaChatApproach.class, approach);
    }

    @Test
    void testCreateApproachWithInvalidApproachName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ragApproachFactory.createApproach("invalid", RAGType.ASK, null));
    }

    @Test
    void testCreateApproachWithInvalidCombination() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ragApproachFactory.createApproach("rtr", RAGType.CHAT, null));
    }
}
