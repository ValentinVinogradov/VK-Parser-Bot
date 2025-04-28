package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.FullUserInfoDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {
    User getUserByTgId(Long tgUserId);
    Mono<Void> syncUserMarkets(VkAccount vkAccount, List<VkMarket> vkMarkets);
    Boolean existsUserByTgId(Long tgUserId);
    User createUser(Long tgUserId);
    void saveUser(User user);

    FullUserInfoDTO getFullUserInfo(Long tgUserId);
}
