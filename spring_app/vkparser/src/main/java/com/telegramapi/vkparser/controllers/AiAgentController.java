package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.enums.ResponseStatusEnum;
import com.telegramapi.vkparser.impl.AiAgentServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiAgentController {
    private static final Logger log = LoggerFactory.getLogger(AiAgentController.class);
    private final AiAgentServiceImpl aiAgentService;

    public AiAgentController(AiAgentServiceImpl aiAgentService) {
        this.aiAgentService = aiAgentService;
    }


    //todo дописать метод этот
    @GetMapping("/ask-products")
    public Mono<Map<String, String>> askProducts(
            @RequestParam(name = "tg_id") Long tgUserId
    ) throws IOException {
        return aiAgentService.askAiAgent(tgUserId)
                .doOnNext(result -> log.info("AI agent result for tgUserId {}: {}", tgUserId, result)) // <-- логирование
                .map(result -> Map.of(
                        "status", ResponseStatusEnum.SUCCESS.toString(),
                        "message", result
                ))
                .onErrorResume(ex -> {
                    log.error("Error while requesting AI agent for tgUserId {}: {}", tgUserId, ex.getMessage(), ex);
                    return Mono.just(Map.of(
                            "status", ResponseStatusEnum.ERROR.toString(),
                            "message", "Ошибка при запросе: " + ex.getMessage()
                    ));
                });
    }
}
