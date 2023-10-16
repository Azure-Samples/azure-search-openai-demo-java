package com.microsoft.openai.samples.rag.controller;

public record ResponseChoice(int index, ResponseMessage message, ResponseContext context) {
}
