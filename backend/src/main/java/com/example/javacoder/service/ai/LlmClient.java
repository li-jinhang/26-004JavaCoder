package com.example.javacoder.service.ai;

public interface LlmClient {

    String completeJson(String systemPrompt, String userPrompt);
}
