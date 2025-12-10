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
import com.llm.toon.log.Log;

import reactor.core.publisher.Mono;

/**
 * OpenAI-based implementation of LlmClient.
 * Uses WebClient to call OpenAI's chat/completions endpoint synchronously via .block().
 */
@Component("openAiClientToon")
public class OpenAiClientToon implements LlmClient {

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

    public OpenAiClientToon(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
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

        // 🔥 ULTRA COMPACT TOON PROMPT (≈25 tokens)
        String systemPrompt =
                "Return a TOON object with keys task, location, season, friends, " +
                "recipes, ambiguous. Use null when unknown. No commentary.";

        String userPrompt = "Text: " + text;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0
        );

        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        long startTime = System.currentTimeMillis();
        ChatCompletionResponse response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(apiKey))
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        long elapsedMs = System.currentTimeMillis() - startTime;
        System.out.println("=================TOON===================");
        System.out.println(" Response time: " + elapsedMs + " ms");

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty())
            return null;

        System.out.println(" Prompt tokens: " + response.getUsage().getPrompt_tokens());
        System.out.println(" Completion tokens: " + response.getUsage().getCompletion_tokens());
        System.out.println(" Total tokens: " + response.getUsage().getTotal_tokens());

        // Extract assistant content
        String assistantContent = response.getChoices().get(0).getMessage().getContent().trim();

        System.out.println("\n  TOON output: " + assistantContent);

        // Convert TOON → JSON
        String json = toonToJson(assistantContent);

        // Parse JSON
        Map<String, Object> parsed =
                objectMapper.readValue(json, new TypeReference<>() {});

        // Build response
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

    // Convert Toon-like text → valid JSON
    private static String toonToJson(String toon) {
        if (toon == null) return null;
        return toon.replaceAll("(\\w+)\\s*:", "\"$1\":");
    }
}
