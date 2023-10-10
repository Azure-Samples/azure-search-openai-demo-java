package com.microsoft.openai.samples.rag.chat.controller;

import java.util.List;

public record ChatResponse(List<ResponseChoice> choices) {
}