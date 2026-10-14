package com.microsoft.openai.samples.rag.content;


import java.util.List;
import java.util.Map;

public record IndexClientRequest(String fileOrUrlpath, List<Map<String,String>> metadata) {
}
