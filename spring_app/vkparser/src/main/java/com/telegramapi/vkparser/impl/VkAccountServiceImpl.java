package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.repositories.VkAccountRepository;
import com.telegramapi.vkparser.services.VkAccountService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VkAccountServiceImpl implements VkAccountService {
    private final VkAccountRepository vkAccountRepository;

    public VkAccountServiceImpl(VkAccountRepository vkAccountRepository) {
        this.vkAccountRepository = vkAccountRepository;
    }

    public void saveVkAccount(VkAccount vkAccount) {
        vkAccountRepository.save(vkAccount);
    }

    public VkAccount createVkAccount(
            Long vkUserId,
            String accessToken,
            String refreshToken,
            LocalDateTime expiresAt,
            User user) {
        VkAccount vkAccount = new VkAccount();
        vkAccount.setVkUserId(vkUserId);
        vkAccount.setAccessToken(accessToken);
        vkAccount.setRefreshToken(refreshToken);
        vkAccount.setExpiresAt(expiresAt);
        vkAccount.setUser(user);
        return vkAccount;
    }

    @Override
    public List<VkAccount> getAllUserVkAccounts(Long tgUserId) {
        return List.of();
    }
}
