package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkProductAIRequestDTO;
import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.dto.VkProductResponseDTO;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.models.VkProduct;

import java.util.List;
import java.util.UUID;

public interface VkProductService {
    VkProductResponseDTO getVkProductsFromDatabase(VkMarket vkMarket, int count, int page);

    List<VkProduct> getAllVkProductsFromDatabase(VkMarket vkMarket);

    void syncProducts(VkMarket vkMarket, VkAccountCacheDTO vkDTO);

    VkProductResponseDTO getVkProducts(Long tgUserId, int count, int page);

    void saveVkProduct(VkProduct vkProduct);

    VkProductDTO getVkProductById(UUID vkProductId);

    List<VkProductAIRequestDTO> getAllVkProductsForAI(Long tgUserId);
}
