package com.telegramapi.vkparser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VkProductDTO (
    UUID id,
    String title,
    String category,
    String description,
    String price,
    Integer availability,
    @JsonProperty("photo_urls") List<String> photoUrls,
    @JsonProperty("stock_quantity") Integer stockQuantity,
    @JsonProperty("likes_count") Integer likesCount,
    @JsonProperty("reposts_count") Integer repostCount,
    @JsonProperty("views_count") Integer viewsCount,
    @JsonProperty("reviews_count") Integer reviewsCount,
    @JsonProperty("created_at") Instant createdAt
) {}
