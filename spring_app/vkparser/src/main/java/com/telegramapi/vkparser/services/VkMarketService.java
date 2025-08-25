package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkMarketDTO;
import com.telegramapi.vkparser.models.VkMarket;

import java.util.List;
import java.util.UUID;

public interface VkMarketService {
    Boolean existsByVkId(Long vkMarketId);
    VkMarket createVkMarket(Long vkMarketId, String vkMarketName, String vkMarketUrl);
    void saveVkMarket(VkMarket vkMarket);
    VkMarket getMarketById(Long vkMarketId);
    List<VkMarketDTO> getVkMarketsFromCache(Long tgUserId);
    VkMarket getActiveVkMarket(Long tgUserId, VkAccountCacheDTO cacheVkAccount);
    List<VkMarketDTO> getVkMarketListDTO(UUID activeAccountId);


}
