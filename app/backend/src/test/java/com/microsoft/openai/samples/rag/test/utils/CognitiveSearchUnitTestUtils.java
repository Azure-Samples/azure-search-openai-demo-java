// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.test.utils;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import reactor.core.publisher.Mono;

public class CognitiveSearchUnitTestUtils {

    private final HttpHeaders httpHeaders =
            new HttpHeaders()
                    .set(HttpHeaderName.fromString("header1"), "value1")
                    .set(HttpHeaderName.fromString("header2"), "value2");
    private final HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, "http://localhost");
    private final String deserializedHeaders = "header1,value1,header2,value2";

    private List<PagedResponse<Integer>> pagedResponses;

    public <T> PagedFlux<T> getPagedFlux(
            int numberOfPages, Function<Integer, List<T>> valueSupplier) {
        List<PagedResponse<T>> pagedResponses =
                IntStream.range(0, numberOfPages)
                        .boxed()
                        .map(
                                i ->
                                        createPagedResponse(
                                                httpRequest,
                                                httpHeaders,
                                                deserializedHeaders,
                                                numberOfPages,
                                                valueSupplier,
                                                i))
                        .collect(Collectors.toList());

        return new PagedFlux<>(
                () -> pagedResponses.isEmpty() ? Mono.empty() : Mono.just(pagedResponses.get(0)),
                continuationToken -> getNextPage(continuationToken, pagedResponses));
    }

    public SearchPagedFlux getSearchPagedFlux(
            int numberOfPages, Function<Integer, List<SearchResult>> valueSupplier) {
        List<SearchPagedResponse> searchPagedResponses =
                IntStream.range(0, numberOfPages)
                        .boxed()
                        .map(
                                i ->
                                        createSearchPagedResponse(
                                                httpRequest,
                                                httpHeaders,
                                                deserializedHeaders,
                                                numberOfPages,
                                                valueSupplier,
                                                i))
                        .collect(Collectors.toList());

        return new SearchPagedFlux(
                () ->
                        searchPagedResponses.isEmpty()
                                ? Mono.empty()
                                : Mono.just(searchPagedResponses.get(0)),
                continuationToken -> getNextSearchPage(continuationToken, searchPagedResponses));
    }

    private <T> PagedResponseBase<String, T> createPagedResponse(
            HttpRequest httpRequest,
            HttpHeaders headers,
            String deserializedHeaders,
            int numberOfPages,
            Function<Integer, List<T>> valueSupplier,
            int i) {
        return new PagedResponseBase<>(
                httpRequest,
                200,
                headers,
                valueSupplier.apply(i),
                (i < numberOfPages - 1) ? String.valueOf(i + 1) : null,
                deserializedHeaders);
    }

    private SearchPagedResponse createSearchPagedResponse(
            HttpRequest httpRequest,
            HttpHeaders headers,
            String deserializedHeaders,
            int numberOfPages,
            Function<Integer, List<SearchResult>> valueSupplier,
            int i) {
        String continuationToken = (i < numberOfPages - 1) ? String.valueOf(i + 1) : null;
        PagedResponseBase pagedResponseBase =
                new PagedResponseBase<>(
                        httpRequest,
                        200,
                        headers,
                        valueSupplier.apply(i),
                        continuationToken,
                        deserializedHeaders);

        return new SearchPagedResponse(pagedResponseBase, continuationToken, null, 0L, 0.0);
    }

    private <T> Mono<PagedResponse<T>> getNextPage(
            String continuationToken, List<PagedResponse<T>> pagedResponses) {

        if (continuationToken == null || continuationToken.isEmpty()) {
            return Mono.empty();
        }

        int parsedToken = Integer.parseInt(continuationToken);
        if (parsedToken >= pagedResponses.size()) {
            return Mono.empty();
        }

        return Mono.just(pagedResponses.get(parsedToken));
    }

    private Mono<SearchPagedResponse> getNextSearchPage(
            String continuationToken, List<SearchPagedResponse> searchPagedResponses) {

        if (continuationToken == null || continuationToken.isEmpty()) {
            return Mono.empty();
        }

        int parsedToken = Integer.parseInt(continuationToken);
        if (parsedToken >= searchPagedResponses.size()) {
            return Mono.empty();
        }

        return Mono.just(searchPagedResponses.get(parsedToken));
    }

    public void setSearchDocument(SearchDocument searchDocument, SearchResult searchResult) {

        Method setadditionalPropertiesMethod;
        Method setJsonSerializerMethod;
        DefaultJsonSerializer defaultJsonSerializer = new DefaultJsonSerializer();
        try {
            setadditionalPropertiesMethod =
                    SearchResult.class.getDeclaredMethod(
                            "setAdditionalProperties", SearchDocument.class);
            setJsonSerializerMethod =
                    SearchResult.class.getDeclaredMethod("setJsonSerializer", JsonSerializer.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find method in SearchResult class", e);
        }
        setadditionalPropertiesMethod.setAccessible(true);
        setJsonSerializerMethod.setAccessible(true);
        try {
            setadditionalPropertiesMethod.invoke(searchResult, searchDocument);
            setJsonSerializerMethod.invoke(searchResult, defaultJsonSerializer);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
