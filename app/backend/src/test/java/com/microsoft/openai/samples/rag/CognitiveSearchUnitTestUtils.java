package com.microsoft.openai.samples.rag;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.http.rest.PagedResponseBase;
import com.azure.core.implementation.serializer.DefaultJsonSerializer;
import com.azure.core.util.serializer.JsonSerializer;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedFlux;
import com.azure.search.documents.util.SearchPagedResponse;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CognitiveSearchUnitTestUtils {
    private final HttpHeaders httpHeaders = new HttpHeaders()
            .set(HttpHeaderName.fromString("header1"), "value1")
            .set(HttpHeaderName.fromString("header2"), "value2");
    private final HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, "http://localhost");
    private final String deserializedHeaders = "header1,value1,header2,value2";

    private List<PagedResponse<Integer>> pagedResponses;

    public <T> PagedFlux<T> getPagedFlux(int numberOfPages,Function<Integer, List<T>> valueSupplier) {
        List<PagedResponse<T>> pagedResponses = IntStream.range(0, numberOfPages)
                .boxed()
                .map(i ->
                        createPagedResponse(httpRequest, httpHeaders, deserializedHeaders, numberOfPages, valueSupplier, i))
                .collect(Collectors.toList());

        return new PagedFlux<>(() -> pagedResponses.isEmpty() ? Mono.empty() : Mono.just(pagedResponses.get(0)),
                continuationToken -> getNextPage(continuationToken, pagedResponses));
    }

    public SearchPagedFlux getSearchPagedFlux(int numberOfPages, Function<Integer, List<SearchResult>> valueSupplier) {
        List<SearchPagedResponse> searchPagedResponses = IntStream.range(0, numberOfPages)
                .boxed()
                .map(i ->
                        createSearchPagedResponse(httpRequest, httpHeaders, deserializedHeaders, numberOfPages, valueSupplier, i))
                .collect(Collectors.toList());

        return new SearchPagedFlux(() -> searchPagedResponses.isEmpty() ? Mono.empty() : Mono.just(searchPagedResponses.get(0)),
                continuationToken -> getNextSearchPage(continuationToken, searchPagedResponses));
    }

    private <T> PagedResponseBase<String, T> createPagedResponse(HttpRequest httpRequest, HttpHeaders headers,
                                                                 String deserializedHeaders,
                                                                 int numberOfPages,
                                                                 Function<Integer, List<T>> valueSupplier,
                                                                 int i) {
        return new PagedResponseBase<>(httpRequest, 200, headers, valueSupplier.apply(i),
                (i < numberOfPages - 1) ? String.valueOf(i + 1) : null,
                deserializedHeaders);
    }



    private SearchPagedResponse createSearchPagedResponse(HttpRequest httpRequest, HttpHeaders headers,
                                                                 String deserializedHeaders,
                                                                 int numberOfPages,
                                                                 Function<Integer, List<SearchResult>> valueSupplier,
                                                                 int i) {
        PagedResponseBase pagedResponseBase = new PagedResponseBase<>(httpRequest, 200, headers, valueSupplier.apply(i),
                (i < numberOfPages - 1) ? String.valueOf(i + 1) : null,
                deserializedHeaders);

        return new SearchPagedResponse(pagedResponseBase,(i < numberOfPages - 1) ? String.valueOf(i + 1) : null,null,Long.valueOf(0),0.0);
    }



    private <T> Mono<PagedResponse<T>> getNextPage(String continuationToken,
                                                     List<PagedResponse<T>> pagedResponses) {

        if (continuationToken == null || continuationToken.isEmpty()) {
            return Mono.empty();
        }

        int parsedToken = Integer.parseInt(continuationToken);
        if (parsedToken >= pagedResponses.size()) {
            return Mono.empty();
        }

        return Mono.just(pagedResponses.get(parsedToken));
    }

    private Mono<SearchPagedResponse> getNextSearchPage(String continuationToken,
                                                   List<SearchPagedResponse> searchPagedResponses) {

        if (continuationToken == null || continuationToken.isEmpty()) {
            return Mono.empty();
        }

        int parsedToken = Integer.parseInt(continuationToken);
        if (parsedToken >= searchPagedResponses.size()) {
            return Mono.empty();
        }

        return Mono.just(searchPagedResponses.get(parsedToken));
    }

    public void setSearchDocument (SearchDocument searchDocument, SearchResult searchResult){

        Method setadditionalPropertiesMethod = null;
        Method setJsonSerializerMethod = null;
        DefaultJsonSerializer defaultJsonSerializer = new DefaultJsonSerializer();
        try {
            setadditionalPropertiesMethod = SearchResult.class.getDeclaredMethod("setAdditionalProperties", SearchDocument.class);
            setJsonSerializerMethod = SearchResult.class.getDeclaredMethod("setJsonSerializer", JsonSerializer.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find method in SearchResult class", e);
        }
        setadditionalPropertiesMethod.setAccessible(true);
        setJsonSerializerMethod.setAccessible(true);
        try {
            setadditionalPropertiesMethod.invoke(searchResult,searchDocument);
            setJsonSerializerMethod.invoke(searchResult,defaultJsonSerializer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) {
    var utils = new CognitiveSearchUnitTestUtils();


    Map<String,String> cit1HasDoc = new HashMap<>();
    cit1HasDoc.put("content","This is a test document 1 for the unit test");
    cit1HasDoc.put("sourcepage","cit1.pdf");


        SearchDocument cit1Document = new SearchDocument(cit1HasDoc);
        SearchResult cit1SearchResult = new SearchResult(0.6);
        utils.setSearchDocument(cit1Document,cit1SearchResult);

        Map<String,String> cit2HasDoc = new HashMap<>();
        cit1HasDoc.put("content","This is a test document 2 for the unit test");
        cit1HasDoc.put("sourcepage","cit2.pdf");


        SearchDocument cit2Document = new SearchDocument(cit1HasDoc);
        SearchResult cit2SearchResult = new SearchResult(0.6);
        utils.setSearchDocument(cit2Document,cit2SearchResult);

        SearchPagedFlux searchPagedFlux = utils.getSearchPagedFlux(1,(inputInteger) -> {return List.of(cit1SearchResult,cit2SearchResult);});
        searchPagedFlux.subscribe(item -> System.out.println("Processing item with value: " + item.getDocument(SearchDocument.class).get("content")),
                error -> System.err.println("An error occurred: " + error),
                () -> System.out.println("Processing complete."));

    }



}
