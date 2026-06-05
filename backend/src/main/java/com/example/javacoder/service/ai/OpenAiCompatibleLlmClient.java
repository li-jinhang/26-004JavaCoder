package com.example.javacoder.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        validateConfiguration();

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", properties.getModel(),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri())
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LlmException(HttpStatus.BAD_GATEWAY, "LLM request failed with HTTP " + response.statusCode() + ".");
            }
            return extractContent(response.body());
        } catch (JsonProcessingException exception) {
            throw new LlmException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize LLM request.", exception);
        } catch (IOException exception) {
            throw new LlmException(HttpStatus.BAD_GATEWAY, "Failed to call LLM service.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmException(HttpStatus.BAD_GATEWAY, "LLM request was interrupted.", exception);
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new LlmException(HttpStatus.SERVICE_UNAVAILABLE, "LLM is disabled.");
        }
        if (properties.getBaseUrl().isBlank() || properties.getApiKey().isBlank() || properties.getModel().isBlank()) {
            throw new LlmException(HttpStatus.SERVICE_UNAVAILABLE, "LLM is not configured.");
        }
    }

    private URI chatCompletionsUri() {
        String baseUrl = properties.getBaseUrl();
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized);
        }
        return URI.create(normalized + "/chat/completions");
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new LlmException(HttpStatus.BAD_GATEWAY, "LLM response did not include message content.");
            }
            return content.asText();
        } catch (JsonProcessingException exception) {
            throw new LlmException(HttpStatus.BAD_GATEWAY, "Failed to parse LLM response.", exception);
        }
    }
}
