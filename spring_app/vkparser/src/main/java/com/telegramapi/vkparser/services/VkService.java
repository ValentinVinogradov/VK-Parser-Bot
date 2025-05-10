package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.models.VkProduct;
import reactor.core.publisher.Mono;

import java.util.List;

public interface VkService {
    Mono<VkTokenResponseDTO> getUserTokens(String code, String state, String deviceId);
    Mono<List<VkMarket>> getUserMarkets(Long vkUserId, String accessToken);
    Mono<List<VkProduct>> getProducts(String accessToken, Long vkMarketId);
}
