package com.microsoft.openai.samples.rag.chat.controller;

import java.util.List;

public record ResponseContext(String thoughts, List<String> data_points) {
}