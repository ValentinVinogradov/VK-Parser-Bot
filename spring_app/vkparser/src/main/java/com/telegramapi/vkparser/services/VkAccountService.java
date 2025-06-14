package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;

import java.time.LocalDateTime;
import java.util.List;

public interface VkAccountService {
    void saveVkAccount(VkAccount vkAccount);
    VkAccount createVkAccount(
            Long vkUserId,
            String accessToken,
            String refreshToken,
            String idToken,
            String deviceId,
            LocalDateTime expiresAt,
            User user);
    List<VkAccount> getAllUserVkAccounts(Long tgUserId);
}
