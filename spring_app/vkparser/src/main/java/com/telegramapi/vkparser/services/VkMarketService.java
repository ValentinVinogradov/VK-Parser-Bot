package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.models.VkMarket;

public interface VkMarketService {
    Boolean existsByVkId(Long vkMarketId);
    VkMarket getMarketById(Long vkMarketId);
    VkMarket createVkMarket(Long vkMarketId, String vkMarketName);
    VkMarket saveVkMarket(VkMarket vkMarket);
}
