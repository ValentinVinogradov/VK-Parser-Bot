package com.telegramapi.vkparser.utils;

import java.time.Instant;
import java.util.List;

public record VkParsedProduct(
        long vkProductId,
        String title,
        String category,
        String description,
        String price,
        List<String> photoUrls,
        int availability,
        int stockQuantity,
        int likesCount,
        int repostsCount,
        int reviewsCount,
        int viewsCount,
        double rating,
        Instant createdAt
) {
}
