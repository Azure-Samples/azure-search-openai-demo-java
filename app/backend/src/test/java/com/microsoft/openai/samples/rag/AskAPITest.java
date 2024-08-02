// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.azure.ai.openai.models.Choice;
import com.azure.ai.openai.models.Completions;
import com.azure.ai.openai.models.CompletionsOptions;
import com.azure.ai.openai.models.CompletionsUsage;
import com.azure.core.util.Context;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedIterable;
import com.microsoft.openai.samples.rag.proxy.AzureAISearchProxy;
import com.microsoft.openai.samples.rag.proxy.OpenAIProxy;
import com.microsoft.openai.samples.rag.test.utils.CognitiveSearchUnitTestUtils;
import com.microsoft.openai.samples.rag.test.utils.OpenAIUnitTestUtils;
import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * This class tests the Ask API showcasing how you can mock azure services using mockito.
 * CognitiveSearch and OpenAI models are immutable from the client usage perspective, so in order to
 * create when/then condition with mockito we used a reflection hack to make some model private
 * constructor public. @see CognitiveSearchUnitTestUtils and @see OpenAIUnitTestUtils for more info.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AskAPITest {

    @Autowired private TestRestTemplate restTemplate;
    @MockBean private AzureAISearchProxy azureAISearchProxyMock;

    @MockBean private OpenAIProxy openAIProxyMock;

    /**
     * after changing ask implementation from open ai completion to chatcompletion this test breaks.
     * //TODO need to update mocks with ChatCompletions mocks instead of Completions mocks @Test
     * void testExample() { prepareMocks();
     *
     * <p>AskRequest askRequest = new AskRequest(); askRequest.setQuestion("What does a Product
     * Manager do?"); askRequest.setApproach("rtr");
     *
     * <p>HttpEntity<AskRequest> request = new HttpEntity<>(askRequest);
     *
     * <p>ResponseEntity<AskResponse> result = this.restTemplate.postForEntity(uri("/api/ask"),
     * request, AskResponse.class);
     *
     * <p>assertEquals(HttpStatus.OK, result.getStatusCode()); assertNotNull(result.getBody());
     * assertEquals("Product managers put items in roadmaps and backlogs",
     * result.getBody().getAnswer()); assertEquals(2, result.getBody().getDataPoints().size());
     * assertEquals("cit1.pdf: This is a test document 1 for the unit test",
     * result.getBody().getDataPoints().get(0)); assertEquals("cit2.pdf: This is a test document 2
     * for the unit test", result.getBody().getDataPoints().get(1)); }
     */
    private void prepareMocks() {
        SearchPagedIterable searchPagedIterable = buildSearchPagedIterableWithDocs();
        when(azureAISearchProxyMock.search(
                        eq("What does a Product Manager do?"),
                        any(SearchOptions.class),
                        eq(Context.NONE)))
                .thenReturn(searchPagedIterable);

        Completions mockedCompletions = buildCompletions();
        when(openAIProxyMock.getCompletions(any(CompletionsOptions.class)))
                .thenReturn(mockedCompletions);
    }

    private Completions buildCompletions() {
        OpenAIUnitTestUtils utils = new OpenAIUnitTestUtils();
        Choice choice1 =
                utils.createChoice("Product managers put items in roadmaps and backlogs", 0);

        List<Choice> choices = List.of(choice1);

        CompletionsUsage completionsUsage = utils.createCompletionUsage(0, 0, 0);
        return utils.createCompletions(choices, completionsUsage);
    }

    private SearchPagedIterable buildSearchPagedIterableWithDocs() {
        CognitiveSearchUnitTestUtils utils = new CognitiveSearchUnitTestUtils();

        Map<String, String> cit1HasDoc = new HashMap<>();
        cit1HasDoc.put("content", "This is a test document 1 for the unit test");
        cit1HasDoc.put("sourcepage", "cit1.pdf");

        SearchDocument cit1Document = new SearchDocument(cit1HasDoc);
        SearchResult cit1SearchResult = new SearchResult(0.6);
        utils.setSearchDocument(cit1Document, cit1SearchResult);

        cit1HasDoc.put("content", "This is a test document 2 for the unit test");
        cit1HasDoc.put("sourcepage", "cit2.pdf");

        SearchDocument cit2Document = new SearchDocument(cit1HasDoc);
        SearchResult cit2SearchResult = new SearchResult(0.6);
        utils.setSearchDocument(cit2Document, cit2SearchResult);

        return new SearchPagedIterable(
                utils.getSearchPagedFlux(
                        1, (inputInteger) -> List.of(cit1SearchResult, cit2SearchResult)));
    }

    private URI uri(String path) {
        return restTemplate.getRestTemplate().getUriTemplateHandler().expand(path);
    }

    private CompletionsOptions buildCompletionsOptionsWithMockData() {
        CompletionsOptions completionsOptions =
                new CompletionsOptions(new ArrayList<>(Arrays.asList(PROMPT_WITH_MOCKED_SOURCES)));

        // Due to a potential bug in using JVM 17 and java open SDK 1.0.0-beta.2, we need to provide
        // default for all properties to avoid 404 bad Request on the server
        completionsOptions.setMaxTokens(1024);
        completionsOptions.setTemperature(0.3);
        completionsOptions.setStop(new ArrayList<>(Arrays.asList("\n")));
        completionsOptions.setLogitBias(new HashMap<>());
        completionsOptions.setEcho(false);
        completionsOptions.setN(1);
        completionsOptions.setStream(false);
        completionsOptions.setUser("search-openai-demo-java");
        completionsOptions.setPresencePenalty(0.0);
        completionsOptions.setFrequencyPenalty(0.0);
        completionsOptions.setBestOf(1);

        return completionsOptions;
    }

    private static final String PROMPT_WITH_MOCKED_SOURCES =
            """
        You are an intelligent assistant helping Contoso Inc employees with their healthcare plan questions and employee handbook questions.
        Use 'you' to refer to the individual asking the questions even if they ask with 'I'.
        Answer the following question using only the data provided in the sources below.
        For tabular information return it as an html table. Do not return markdown format.
        Each source has a name followed by colon and the actual information, always include the source name for each fact you use in the response.
        If you cannot answer using the sources below, say you don't know.

        ###
        Question: 'What is the deductible for the employee plan for a visit to Overlake in Bellevue?'

        Sources:
        cit1.pdf: This is a test document 1 for the unit test
        cit2.pdf: This is a test document 2 for the unit test

        Answer:
        In-network deductibles are $500 for employee and $1000 for family [info1.txt] and Overlake is in-network for the employee plan [info2.pdf][info4.pdf].

        ###
        Question:'What does a Product Manager do??'

        Sources:
        %s

        Answer:
        """;
}
