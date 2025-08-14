package com.telegramapi.vkparser.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VkAccountCacheDTO(
    UUID id,
    String accessToken,
    String refreshToken,
    String deviceId,
    LocalDateTime expiresAt
) {
}
