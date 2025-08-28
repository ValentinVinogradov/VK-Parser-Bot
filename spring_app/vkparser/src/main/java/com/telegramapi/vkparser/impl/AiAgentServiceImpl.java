package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.config.WebClientFactory;
import com.telegramapi.vkparser.dto.G4fResponseDTO;
import com.telegramapi.vkparser.dto.VkProductAIRequestDTO;
import com.telegramapi.vkparser.services.VkProductService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiAgentServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(AiAgentServiceImpl.class);


    private final String AI_AGENT_CONTAINER_URL = System.getenv("AI_AGENT_CONTAINER_URL");
    private final String SYSTEM_PROMPT_PATH = System.getenv("SYSTEM_PRODUCT_PROMPT_PATH");

    private final WebClient aiWebClient;
    private final VkProductService vkProductService;


    public AiAgentServiceImpl(WebClientFactory webClientFactory, VkProductService vkProductService) {
        this.aiWebClient = webClientFactory.create(AI_AGENT_CONTAINER_URL);
        this.vkProductService = vkProductService;
    }

    public String loadSystemPrompt() throws IOException {
        var resource = new ClassPathResource(SYSTEM_PROMPT_PATH);
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }



    private String makePromptFromProducts(Long tgUserId) throws IOException {
        List<VkProductAIRequestDTO> productsDTO = vkProductService.getAllVkProductsForAI(tgUserId);


        if (productsDTO == null || productsDTO.isEmpty()) {
            return "Nothing to prompt";
        }

        return productsDTO.stream()
                .map(p -> String.format(
                        "Товар: %s, цена: %s, просмотры: %d, лайки: %d, репосты: %d, отзывы: %d, создан: %s",
                        p.title(),
                        p.price(),
                        p.viewsCount(),
                        p.likesCount(),
                        p.repostCount(),
                        p.reviewsCount(),
                        p.createdAt()
                ))
                .collect(Collectors.joining("\n"));
    }



    public Mono<String> askAiAgent(Long tgUserId) throws IOException {
        String systemPrompt = loadSystemPrompt();
        String userPrompt = makePromptFromProducts(tgUserId);

        log.info("Prepare user prompt for AI={}", userPrompt);
        log.info("Prepare system prompt for AI={}", systemPrompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4.1");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
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

