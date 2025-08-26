package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.config.WebClientFactory;
import com.telegramapi.vkparser.dto.G4fResponseDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAgentServiceImpl {
    private final String AI_AGENT_CONTAINER_URL = System.getenv("AI_AGENT_CONTAINER_URL");

    private final WebClient aiWebClient;


    public AiAgentServiceImpl(WebClientFactory webClientFactory) {
        this.aiWebClient = webClientFactory.create(AI_AGENT_CONTAINER_URL);
    }



    public Mono<String> askAiAgent(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4.1");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are helpful assistant."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("web_search", false);
//        requestBody.put("provider", "PollinationsAI");

        //todo логи и протестить
         return aiWebClient.post()
                 .uri("/v1/chat/completions")
                 .contentType(MediaType.APPLICATION_JSON)
                 .bodyValue(requestBody)
                 .retrieve()
                 .bodyToMono(G4fResponseDTO.class)
                 .doOnSubscribe(sub -> System.out.println("Sending request to AI agent..."))
                 .doOnError(e -> System.out.println("Error sending request to AI agent: " + e.getMessage()))
                 .doOnNext(resp -> System.out.println("Received response from AI agent"))
                 .doOnSuccess(s -> System.out.println("Norm"))
                 .map(resp -> {
                     if (resp.choices() == null || resp.choices().isEmpty()) return "";
                     G4fResponseDTO.Message msg = resp.choices().getFirst().message();
                     return msg != null ? msg.content() : "";
                 });
        }
}

