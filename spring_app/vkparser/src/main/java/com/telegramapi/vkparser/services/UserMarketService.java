package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.models.UserMarket;

import java.util.List;

public interface UserMarketService {
    void saveAllUserMarkets(List<UserMarket> userMarkets);
    // List<UserMarket> getAllUserMarkets(VkAccount vkAccount);
}
