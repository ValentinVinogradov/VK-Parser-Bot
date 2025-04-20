package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkMarketDTO;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.UserMarket;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {
    User getUserByTgId(Long tgUserId);
    Mono<Void> syncUserMarkets(Long tgUserId, Long vkUserId, String accessToken);
    Boolean existsUserByTgId(Long tgUserId);
    User createUser(Long tgUserId);
    User saveUser(User user);
}
