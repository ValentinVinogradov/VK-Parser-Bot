package com.telegramapi.vkparser.dto;

import java.util.UUID;

public record VkAccountCacheDTO(
    UUID id,
    String accessToken
) {
}
