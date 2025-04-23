package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.repositories.VkAccountRepository;
import com.telegramapi.vkparser.services.VkAccountService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VkAccountServiceImpl implements VkAccountService {
    private final VkAccountRepository vkAccountRepository;


    public VkAccountServiceImpl(VkAccountRepository vkAccountRepository) {
        this.vkAccountRepository = vkAccountRepository;
    }

    public VkAccount getVkAccountById(UUID vkAccountId) {
        return vkAccountRepository.findById(vkAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Vk account not found!"));
    }

    public void saveVkAccount(VkAccount vkAccount) {
        vkAccountRepository.save(vkAccount);
    }

    public VkAccount createVkAccount(
            Long vkUserId,
            String accessToken,
            String refreshToken,
            String idToken,
            LocalDateTime expiresAt,
            User user) {
        VkAccount vkAccount = new VkAccount();
        vkAccount.setVkUserId(vkUserId);
        vkAccount.setAccessToken(accessToken);
        vkAccount.setRefreshToken(refreshToken);
        vkAccount.setIdToken(idToken);
        vkAccount.setExpiresAt(expiresAt);
        vkAccount.setUser(user);
        return vkAccount;
    }

    @Override
    public List<VkAccount> getAllUserVkAccounts(Long tgUserId) {
        return List.of();
    }

    public VkAccount getActiveAccount(User user) {
        return vkAccountRepository.findByUserAndIsActiveTrue(user)
                .orElseThrow(() -> new EntityNotFoundException("Active vk account not found!"));
    }


    public void setActiveVkAccount(UUID vkAccountId, User user) {
        vkAccountRepository.deactivateVkAccount(user);
        VkAccount vkAccount = getVkAccountById(vkAccountId);
        vkAccount.setActive(true);
        vkAccountRepository.save(vkAccount);
    }
}
