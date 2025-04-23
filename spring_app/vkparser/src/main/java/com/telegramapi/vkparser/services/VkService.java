package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkMarket;
import reactor.core.publisher.Mono;

import java.util.List;

public interface VkService {
    Mono<VkTokenResponseDTO> getUserTokens(String code, String state, String deviceId);
    Mono<List<VkMarket>> getUserMarkets(Long tgUserId, Long vkUserId, String accessToken);
}
