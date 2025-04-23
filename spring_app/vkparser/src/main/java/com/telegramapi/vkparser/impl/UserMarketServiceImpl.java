package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.UserMarket;
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

    public void saveAllUserMarkets(List<UserMarket> userMarkets) {
        userMarketRepository.saveAll(userMarkets);
    }

    public void saveUserMarket(UserMarket userMarket) {
        userMarketRepository.save(userMarket);
    }

    @Override
    public List<UserMarket> getAllUserMarkets(Long vkUserId) {
        return userMarketRepository.findAllByVkUserId(vkUserId);
    }
}
