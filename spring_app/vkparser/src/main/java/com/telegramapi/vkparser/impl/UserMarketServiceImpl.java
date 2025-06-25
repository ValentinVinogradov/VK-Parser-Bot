package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.UserMarketRepository;
import com.telegramapi.vkparser.services.UserMarketService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserMarketServiceImpl implements UserMarketService {
    private static final Logger log = LoggerFactory.getLogger(UserMarketServiceImpl.class);

    private final UserMarketRepository userMarketRepository;

    public UserMarketServiceImpl(UserMarketRepository userMarketRepository) {
        this.userMarketRepository = userMarketRepository;
    }

    public UserMarket createUserMarket(VkAccount vkAccount, VkMarket vkMarket) {
        UserMarket userMarket = new UserMarket();
        userMarket.setVkAccount(vkAccount);
        userMarket.setVkMarket(vkMarket);
        log.debug("Created new UserMarket instance for VK account ID: {}, VK market ID: {}",
                vkAccount.getId(), vkMarket.getId());
        return userMarket;
    }

    public void saveAllUserMarkets(List<UserMarket> userMarkets) {
        userMarketRepository.saveAll(userMarkets);
        log.info("Saved {} UserMarket entries", userMarkets.size());
    }

    public void saveUserMarket(UserMarket userMarket) {
        userMarketRepository.save(userMarket);
        log.info("Saved UserMarket with VK market ID: {} and VK account ID: {}",
                userMarket.getVkMarket().getId(), userMarket.getVkAccount().getId());
    }

    public List<UserMarket> getAllUserMarkets(UUID vkAccountId) {
        List<UserMarket> markets = userMarketRepository.findAllByVkAccount_Id(vkAccountId);
        log.info("Fetched {} UserMarkets for VK account ID: {}", markets.size(), vkAccountId);
        return markets;
    }

    // @Override
    // public List<UserMarket> getAllUserMarkets(VkAccount vkAccount) {
    //     List<UserMarket> markets = userMarketRepository.findAllByVkAccount(vkAccount);
    //     log.info("Fetched {} UserMarkets for VK account ID: {}", markets.size(), vkAccount.getId());
    //     return markets;
    // }

    public UserMarket getUserMarketById(Long vkMarketId) {
        return userMarketRepository.findById(vkMarketId)
                .map(userMarket -> {
                    log.info("Found UserMarket with ID: {}", vkMarketId);
                    return userMarket;
                })
                .orElseGet(() -> {
                    log.warn("UserMarket with ID {} not found", vkMarketId);
                    return null;
                });
    }

    public UserMarket getActiveUserMarket(UUID vkAccountId) {
        return userMarketRepository.findByVkAccount_IdAndIsActiveTrue(vkAccountId)
                .map(userMarket -> {
                    log.info("Found active UserMarket for VK account ID: {}", vkAccountId);
                    return userMarket;
                })
                .orElseGet(() -> {
                    log.warn("No active UserMarket found for VK account ID: {}", vkAccountId);
                    return null;
                });
    }

    @Transactional
    public void setActiveUserMarket(UUID vkAccountId, UUID userMarketId) {
        log.info("Setting UserMarket with ID: {} as active for VK account ID: {}", userMarketId, vkAccountId);
        userMarketRepository.deactivateUserMarket(vkAccountId);
        userMarketRepository.activateUserMarket(userMarketId);
        log.info("UserMarket activation completed for VK account ID: {}", vkAccountId);
    }
}
