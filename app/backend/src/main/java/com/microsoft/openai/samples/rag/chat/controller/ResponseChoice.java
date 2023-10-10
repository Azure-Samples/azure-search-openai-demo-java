package com.microsoft.openai.samples.rag.chat.controller;

public record ResponseChoice(int index, ResponseMessage message, ResponseContext context) {
}
