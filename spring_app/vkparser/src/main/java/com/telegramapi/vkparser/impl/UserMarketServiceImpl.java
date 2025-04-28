package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.UserMarketRepository;
import com.telegramapi.vkparser.services.UserMarketService;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public UserMarket getActiveUserMarket(VkAccount vkAccount) {
        return userMarketRepository.findByVkAccountAndIsActiveTrue(vkAccount)
                .orElse(null);
    }
}
