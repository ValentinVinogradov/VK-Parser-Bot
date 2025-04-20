package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.VkMarketRepository;
import com.telegramapi.vkparser.services.VkMarketService;
import org.springframework.stereotype.Service;

@Service
public class VkMarketServiceImpl implements VkMarketService {
    private final VkMarketRepository vkMarketRepository;

    public VkMarketServiceImpl(VkMarketRepository vkMarketRepository) {
        this.vkMarketRepository = vkMarketRepository;
    }

    public Boolean existsByVkId(Long vkMarketId) {
        return vkMarketRepository.existsByMarketVkId(vkMarketId);
    }



    public VkMarket getMarketById(Long vkMarketId) {
        return vkMarketRepository.getByMarketVkId(vkMarketId);
    }

    public VkMarket createVkMarket(Long vkMarketId, String vkMarketName) {
        VkMarket vkMarket = new VkMarket();
        vkMarket.setMarketName(vkMarketName);
        vkMarket.setMarketVkId(vkMarketId);
        return vkMarket;
    }

    public VkMarket saveVkMarket(VkMarket vkMarket) {
        return vkMarketRepository.save(vkMarket);
    }
}
