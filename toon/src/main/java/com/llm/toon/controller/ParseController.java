package com.llm.toon.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.llm.toon.dto.ParseResponse;
import com.llm.toon.llm.LlmClient;
import com.llm.toon.llm.OpenAiClient;
import com.llm.toon.llm.OpenAiClientToon;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class ParseController {

    private final LlmClient llmClient;
    private final OpenAiClient openAiClient;
    private final OpenAiClientToon openAiClientToon;

    public ParseController(@Qualifier("openAiClient")LlmClient llmClient,OpenAiClient openAiClient,OpenAiClientToon openAiClientToon) {
        this.llmClient = llmClient;
        this.openAiClient = openAiClient;
        this.openAiClientToon = openAiClientToon;
    }

    @PostMapping("/api/parse")
    public Mono<ParseResponse> parse(@RequestBody String text) {
        return Mono.fromCallable(() -> openAiClient.parseSalesText(text))
                   .subscribeOn(Schedulers.boundedElastic());
    }
    
    @PostMapping("/api/parsetoon")
    public Mono<ParseResponse> parsetoon(@RequestBody String text) {
        return Mono.fromCallable(() -> openAiClientToon.parseSalesText(text))
                   .subscribeOn(Schedulers.boundedElastic());
    }
    

}