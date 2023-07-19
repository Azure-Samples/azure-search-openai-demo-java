package com.microsoft.openai.samples.rag;

import com.azure.ai.openai.models.*;
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
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OpenAIUnitTestUtils {


    public Choice createChoice (String text, int index ){
        Constructor<Choice> pcc = null;
        try {
            pcc = Choice.class.getDeclaredConstructor(String.class, int.class, CompletionsLogProbabilityModel.class, CompletionsFinishReason.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for Choice.class",e);
        }
        pcc.setAccessible(true);
        Choice choice = null;
        try {
            choice = pcc.newInstance(text,index,null,CompletionsFinishReason.STOPPED);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Choice Instance",e);
        }

        return choice;
    }

    public CompletionsUsage createCompletionUsage (int completionTokens, int promptTokens, int totalTokens){
        Constructor<CompletionsUsage> pcc = null;
        try {
            pcc = CompletionsUsage.class.getDeclaredConstructor(int.class,int.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for CompletionsUsage.class",e);
        }
        pcc.setAccessible(true);
        CompletionsUsage completionsUsage = null;
        try {
            completionsUsage = pcc.newInstance(completionTokens,promptTokens,totalTokens);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create CompletionsUsage Instance",e);
        }

        return completionsUsage;
    }


    public Completions createCompletions (List<Choice> choices){return this.createCompletions(choices,null);}
    public Completions createCompletions (List<Choice> choices, CompletionsUsage completionsUsage){
        Constructor<Completions> pcc = null;
        try {
            pcc = Completions.class.getDeclaredConstructor(String.class,int.class,List.class,CompletionsUsage.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for Completions.class",e);
        }
        pcc.setAccessible(true);
        Completions choice = null;
        try {
            choice = pcc.newInstance(null,0,choices,completionsUsage);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Completions Instance",e);
        }

        return choice;
    }

    public static void main(String[] args) {
    var utils = new OpenAIUnitTestUtils();

    List<Choice> choices = List.of(utils.createChoice(" this a response example",0));
    Completions completions = utils.createCompletions(choices);

    }



}
