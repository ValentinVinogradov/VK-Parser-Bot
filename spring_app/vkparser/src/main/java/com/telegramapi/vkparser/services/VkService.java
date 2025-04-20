package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.models.UserMarket;
import reactor.core.publisher.Mono;

import java.util.List;

public interface VkService {
    Mono<VkTokenResponseDTO> getUserTokens(String code, String state, String deviceId);
    Mono<List<UserMarket>> getUserMarkets(Long tgUserId, Long vkUserId, String accessToken);
}
