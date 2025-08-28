package com.telegramapi.vkparser.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VkProductAIRequestDTO(
        String title,
        String price,
        @JsonProperty("likes_count") Integer likesCount,
        @JsonProperty("reposts_count") Integer repostCount,
        @JsonProperty("views_count") Integer viewsCount,
        @JsonProperty("reviews_count") Integer reviewsCount,
        @JsonProperty("created_at") Instant createdAt
) {
}
