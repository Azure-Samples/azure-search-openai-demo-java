// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.rag.test.utils;

import com.azure.ai.openai.models.*;
import java.lang.reflect.Constructor;
import java.time.OffsetDateTime;
import java.util.List;

public class OpenAIUnitTestUtils {

    public Choice createChoice(String text, int index) {
        Constructor<Choice> pcc;
        try {
            pcc =
                    Choice.class.getDeclaredConstructor(
                            String.class,
                            int.class,
                            CompletionsLogProbabilityModel.class,
                            CompletionsFinishReason.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for Choice.class", e);
        }
        pcc.setAccessible(true);
        Choice choice;
        try {
            choice = pcc.newInstance(text, index, null, CompletionsFinishReason.STOPPED);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Choice Instance", e);
        }

        return choice;
    }

    public CompletionsUsage createCompletionUsage(
            int completionTokens, int promptTokens, int totalTokens) {
        Constructor<CompletionsUsage> pcc;
        try {
            pcc = CompletionsUsage.class.getDeclaredConstructor(int.class, int.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for CompletionsUsage.class", e);
        }
        pcc.setAccessible(true);
        CompletionsUsage completionsUsage;
        try {
            completionsUsage = pcc.newInstance(completionTokens, promptTokens, totalTokens);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create CompletionsUsage Instance", e);
        }

        return completionsUsage;
    }

    public Completions createCompletions(List<Choice> choices) {
        return this.createCompletions(choices, null);
    }

    public Completions createCompletions(List<Choice> choices, CompletionsUsage completionsUsage) {
        Constructor<Completions> pcc;
        try {
            pcc =
                    Completions.class.getDeclaredConstructor(
                            String.class, OffsetDateTime.class, List.class, CompletionsUsage.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor found for Completions.class", e);
        }
        pcc.setAccessible(true);
        Completions choice;
        try {
            choice = pcc.newInstance(null, OffsetDateTime.now(), choices, completionsUsage);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Completions Instance", e);
        }

        return choice;
    }
}
