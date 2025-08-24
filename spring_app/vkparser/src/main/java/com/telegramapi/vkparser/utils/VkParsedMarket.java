package com.telegramapi.vkparser.utils;

public record VkParsedMarket(
        long vkMarketId,
        int membersCount,
        String vkMarketName,
        String vkMarketScreenName
) {
}
