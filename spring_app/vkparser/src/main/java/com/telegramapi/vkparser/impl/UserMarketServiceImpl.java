package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.UserMarketRepository;
import com.telegramapi.vkparser.services.UserMarketService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserMarketServiceImpl implements UserMarketService {
    private final UserMarketRepository userMarketRepository;


    public UserMarketServiceImpl(UserMarketRepository userMarketRepository) {
        this.userMarketRepository = userMarketRepository;
    }

    public UserMarket createUserMarket(VkAccount vkAccount, VkMarket vkMarket) {
        UserMarket userMarket = new UserMarket();
        userMarket.setVkAccount(vkAccount);
        userMarket.setVkMarket(vkMarket);
        return userMarket;
    }

    public void saveAllUserMarkets(List<UserMarket> userMarkets) {
        userMarketRepository.saveAll(userMarkets);
    }

    public void saveUserMarket(UserMarket userMarket) {
        userMarketRepository.save(userMarket);
    }

    @Override
    public List<UserMarket> getAllUserMarkets(VkAccount vkAccount) {
        return userMarketRepository.findAllByVkAccount(vkAccount);
    }

    public UserMarket getUserMarketById(Long vkMarketId) {
        return userMarketRepository.findById(vkMarketId)
                .orElse(null);
    }

    public UserMarket getActiveUserMarket(UUID vkAccountId) {
        return userMarketRepository.findByVkAccount_IdAndIsActiveTrue(vkAccountId)
                .orElse(null);
    }

    @Transactional
    public void setActiveUserMarket(UUID vkAccountId, UUID userMarketId) {
        System.out.println("userMarketId: " + userMarketId);
        System.out.println("vkAccountId: " + vkAccountId);
        userMarketRepository.deactivateUserMarket(vkAccountId);
        userMarketRepository.activateUserMarket(userMarketId);
    }
}
