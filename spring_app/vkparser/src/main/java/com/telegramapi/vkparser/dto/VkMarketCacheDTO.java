package com.telegramapi.vkparser.dto;

import java.util.UUID;

public record VkMarketCacheDTO(
    UUID id,
    Long marketVkId 
) {
}