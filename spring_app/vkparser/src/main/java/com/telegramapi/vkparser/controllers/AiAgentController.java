package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.impl.AiAgentServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiAgentController {
    private final AiAgentServiceImpl aiAgentService;

    public AiAgentController(AiAgentServiceImpl aiAgentService) {
        this.aiAgentService = aiAgentService;
    }


    //todo дописать метод этот
    @GetMapping("/ask-products")
    public Mono<Map<String, String>> askProducts() { return null; }

    //todo мб для магазов сделать сводку тоже по фану, но это не точно
}
