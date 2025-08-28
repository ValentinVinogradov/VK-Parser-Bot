package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkMarketCacheDTO;
import com.telegramapi.vkparser.dto.VkMarketDTO;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.VkMarketRepository;
import com.telegramapi.vkparser.services.VkMarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class VkMarketServiceImpl implements VkMarketService {
    private static final Logger log = LoggerFactory.getLogger(VkMarketServiceImpl.class);

    private final RedisServiceImpl redisService;
    private final UserMarketServiceImpl userMarketService;
    private final VkMarketRepository vkMarketRepository;

    public VkMarketServiceImpl(RedisServiceImpl redisService,
                               UserMarketServiceImpl userMarketService,
                               VkMarketRepository vkMarketRepository) {
        this.redisService = redisService;
        this.userMarketService = userMarketService;
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


    public List<VkMarketDTO> getVkMarketsFromCache(Long tgUserId) {
        return redisService.
                getListValue(String.format("info:%s:vk_markets", tgUserId), VkMarketDTO.class);
    }

    public VkMarket getActiveVkMarket(Long tgUserId, VkAccountCacheDTO cacheVkAccount) {
        log.info("Fetching active VK Market for user tgUserId={}", tgUserId);

        VkMarket activeVkMarket;

        // Try to get from cache first
        VkMarketCacheDTO cachedMarket = redisService
                .getValue(String.format("user:%s:active_vk_market", tgUserId), VkMarketCacheDTO.class);

        if (cachedMarket == null) {
            log.info("No active VK Market found in cache for tgUserId={}, querying database", tgUserId);

            UserMarket userMarket = userMarketService.getActiveUserMarket(cacheVkAccount.id());
            if (userMarket == null) {
                log.error("No active market found for VK account {}", cacheVkAccount.id());
                throw new IllegalStateException("No active market found for VK account");
            }

            activeVkMarket = userMarket.getVkMarket();
            log.info("Active market found in database: marketVkId={}, userMarketId={}",
                    activeVkMarket.getMarketVkId(), userMarket.getId());

            VkMarketCacheDTO cacheMarketDTO = new VkMarketCacheDTO(userMarket.getId(),
                    activeVkMarket.getMarketVkId());

            redisService.setValue(
                    String.format("user:%s:active_vk_market", tgUserId),
                    cacheMarketDTO
            );
            log.info("Saved active VK Market to cache for tgUserId={}", tgUserId);

        } else {
            log.info("Active VK Market found in cache: marketVkId={}", cachedMarket.marketVkId());
            activeVkMarket = getMarketById(cachedMarket.marketVkId());
            log.debug("Loaded VK Market from database by cachedMarket.marketVkId={}", cachedMarket.marketVkId());
        }

        log.info("Returning active VK Market: marketVkId={}", activeVkMarket.getMarketVkId());
        return activeVkMarket;
    }


    public List<VkMarketDTO> getVkMarketListDTO(UUID activeAccountId) {
        List<UserMarket> userMarkets = userMarketService.getAllUserMarkets(activeAccountId);
        return userMarkets
                .stream()
                .map(userMarket -> new VkMarketDTO(
                        userMarket.getId(),
                        userMarket.getVkMarket().getMarketName(),
                        userMarket.getVkMarket().getMembersCount(),
                        userMarket.getActive()
                ))
                .toList();
    }
}
