package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.VkMarketRepository;
import com.telegramapi.vkparser.services.VkMarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VkMarketServiceImpl implements VkMarketService {
    private static final Logger log = LoggerFactory.getLogger(VkMarketServiceImpl.class);

    private final VkMarketRepository vkMarketRepository;

    public VkMarketServiceImpl(VkMarketRepository vkMarketRepository) {
        this.vkMarketRepository = vkMarketRepository;
    }

    public Boolean existsByVkId(Long vkMarketId) {
        boolean exists = vkMarketRepository.existsByMarketVkId(vkMarketId);
        log.info("Checking if VK market with VK ID {} exists: {}", vkMarketId, exists);
        return exists;
    }

    public VkMarket getMarketById(Long vkMarketId) {
        log.info("Retrieving VK market by VK ID: {}", vkMarketId);
        VkMarket market = vkMarketRepository.getByMarketVkId(vkMarketId);
        if (market != null) {
            log.info("VK market found: name='{}', url='{}'", market.getMarketName(), market.getMarketUrl());
        } else {
            log.warn("No VK market found with VK ID: {}", vkMarketId);
        }
        return market;
    }

    public VkMarket createVkMarket(Long vkMarketId, String vkMarketName, String vkMarketUrl) {
        VkMarket vkMarket = new VkMarket();
        vkMarket.setMarketName(vkMarketName);
        vkMarket.setMarketVkId(vkMarketId);
        vkMarket.setMarketUrl(vkMarketUrl);

        log.debug("Created new VK market object: ID={}, Name='{}', URL='{}'",
                vkMarketId, vkMarketName, vkMarketUrl);

        return vkMarket;
    }

    public void saveVkMarket(VkMarket vkMarket) {
        vkMarketRepository.save(vkMarket);
        log.info("Saved VK market with VK ID: {}, name='{}'",
                vkMarket.getMarketVkId(), vkMarket.getMarketName());
    }
}
