package com.llm.toon.llm;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm.toon.dto.ChatCompletionResponse;
import com.llm.toon.dto.ParseResponse;

import reactor.core.publisher.Mono;



import java.util.*;

@Component("openAiClient")
public class OpenAiClient implements LlmClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.llm.provider:mock}")
    private String provider;

    @Value("${app.llm.openai.apiKey:}")
    private String apiKey;

    @Value("${app.llm.openai.baseUrl:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${app.llm.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.llm.openai.timeout-seconds:15}")
    private long timeoutSeconds;

    public OpenAiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public ParseResponse parseSalesText(String text) throws Exception {

        if (!"openai".equalsIgnoreCase(provider)) {
            return null;
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY missing");
        }

        // -----------------------
        // PROMPTS
        // -----------------------
        String systemPrompt =
                "Return a JSON object with keys task, location, season, friends, " +
                "recipes, ambiguous. Use null when unknown. No commentary.";
        
        String userPrompt = "Text:\n" + text + "\nReturn JSON only.";

        // -----------------------
        // Construct request body
        // -----------------------
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0
        );

        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        long start = System.currentTimeMillis();

        ChatCompletionResponse response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(apiKey))
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("=================JSON===================");
        System.out.println(" Response time: " + elapsed + " ms");

        if (response == null ||
            response.getChoices() == null ||
            response.getChoices().isEmpty()) {
            return null;
        }

        // -----------------------
        // Token usage logging
        // -----------------------
        if (response.getUsage() != null) {
            System.out.println(" Prompt  tokens = " + response.getUsage().getPrompt_tokens());
            System.out.println(" Complete tokens = " + response.getUsage().getCompletion_tokens());
            System.out.println(" Total   tokens = " + response.getUsage().getTotal_tokens());
        }

        // -----------------------
        // Extract assistant content
        // -----------------------
        String content = response
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

        if (content == null || content.isBlank()) return null;

        content = stripCodeFences(content);

        // -----------------------
        // Parse JSON into map
        // -----------------------
        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            String jsonSubstring = extractJsonSubstring(content);
            if (jsonSubstring == null) {
                throw new RuntimeException("Failed to parse JSON: " + content, e);
            }
            parsed = objectMapper.readValue(jsonSubstring, new TypeReference<>() {});
        }

        // -----------------------
        // Build ParseResponse
        // -----------------------
        return ParseResponse.builder()
                .productName((String) parsed.get("product_name"))
                .category((String) parsed.get("category"))
                .quantity(parsed.get("quantity") == null ? null :
                        Integer.parseInt(parsed.get("quantity").toString()))
                .saleDate((String) parsed.get("sale_date"))
                .location((String) parsed.get("location"))
                .ambiguous(parsed.get("ambiguous") != null &&
                        Boolean.parseBoolean(parsed.get("ambiguous").toString()))
                .build();
    }

    // ------------------------------------------------
    // Helper: Strip code fences
    // ------------------------------------------------
    private static String stripCodeFences(String text) {
        if (text == null) return null;
        return text.replaceAll("(?s)```(?:json)?\\s*(.*?)\\s*```", "$1").trim();
    }

    // ------------------------------------------------
    // Helper: Extract only {...} JSON block
    // ------------------------------------------------
    private static String extractJsonSubstring(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }
}
