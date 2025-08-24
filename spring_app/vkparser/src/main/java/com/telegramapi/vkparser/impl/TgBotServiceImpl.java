package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.services.TgBotService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class TgBotServiceImpl implements TgBotService {
    private final WebClient vkWebClient;
    private final String TG_BOT_CONTAINER_URL = System.getenv("TG_BOT_CONTAINER_URL");

    public TgBotServiceImpl(WebClient vkWebClient) {
        this.vkWebClient = vkWebClient;
    }

    @Override
    public Mono<String> notifyAuthorizationSuccess(Long tgUserId, VkUserInfoDTO userInfoDTO) {
        // Формируем тело запроса
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tg_id", tgUserId);
        requestBody.put("user_info", userInfoDTO);

        return vkWebClient
                .post()
                .uri(TG_BOT_CONTAINER_URL + "/vk/user_info")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)  // Не ожидаем никакого ответа
                .doOnSubscribe(subscription -> System.out.println("Sending user data to tg bot..."))
                .doOnSuccess(sub -> System.out.println("Successfully sent user data to tg bot!"))
                .doOnError(e -> System.out.println("Error sending data to tg bot: " + e.getMessage()));
    }
}
