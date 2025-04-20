package com.telegramapi.vkparser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    private final String VK_MAIN_URL = System.getenv("VK_MAIN_URL");

    @Bean
    public WebClient vkWebClient() {

        return WebClient.builder()
                .baseUrl(VK_MAIN_URL)
                .build();
    }
}
