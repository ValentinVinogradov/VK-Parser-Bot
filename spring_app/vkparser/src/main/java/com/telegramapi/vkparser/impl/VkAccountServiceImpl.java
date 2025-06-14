package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.repositories.VkAccountRepository;
import com.telegramapi.vkparser.services.VkAccountService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VkAccountServiceImpl implements VkAccountService {
    private static final Logger log = LoggerFactory.getLogger(VkAccountServiceImpl.class);

    private final VkAccountRepository vkAccountRepository;

    public VkAccountServiceImpl(VkAccountRepository vkAccountRepository) {
        this.vkAccountRepository = vkAccountRepository;
    }

    public VkAccount getVkAccountById(UUID id) {
        return vkAccountRepository.findById(id)
                .map(vkAccount -> {
                    log.info("Retrieved VK account with ID: {}", id);
                    return vkAccount;
                })
                .orElseThrow(() -> {
                    log.warn("VK account with {} not found", id);
                    return new EntityNotFoundException("VK account not found!");
                });
    }

    public VkAccount getVkAccountByVkId(Long vkUserId) {
        return vkAccountRepository.findByVkUserId(vkUserId)
                .map(vkAccount -> {
                    log.info("Retrieved VK account with VK ID: {}", vkUserId);
                    return vkAccount;
                })
                .orElseThrow(() -> {
                    log.warn("VK account with ID {} not found", vkUserId);
                    return new EntityNotFoundException("VK account not found!");
                });
    }

    public Boolean existsVkAccountByVkId(Long vkUserId) {
        boolean exists = vkAccountRepository.existsByVkUserId(vkUserId);
        log.info("VK account exists for VK ID {}: {}", vkUserId, exists);
        return exists;
    }

    public void saveVkAccount(VkAccount vkAccount) {
        vkAccountRepository.save(vkAccount);
        log.info("Saved VK account with ID: {}", vkAccount.getId());
    }

    public VkAccount createVkAccount(
            Long vkUserId,
            String accessToken,
            String refreshToken,
            String idToken,
            String deviceId,
            LocalDateTime expiresAt,
            User user) {

        VkAccount vkAccount = new VkAccount();
        vkAccount.setVkUserId(vkUserId);
        vkAccount.setAccessToken(accessToken);
        vkAccount.setRefreshToken(refreshToken);
        vkAccount.setIdToken(idToken);
        vkAccount.setExpiresAt(expiresAt);
        vkAccount.setUser(user);
        vkAccount.setDeviceId(deviceId);

        log.debug("Created new VK account for VK user ID: {} and Telegram user ID: {}",
                vkUserId, user.getTgUserId());

        return vkAccount;
    }

    public VkAccount getActiveAccount(Long tgUserId) {
        return vkAccountRepository.findByUser_TgUserIdAndIsActiveTrue(tgUserId)
                .map(account -> {
                    log.info("Found active VK account for user ID: {}", tgUserId);
                    return account;
                })
                .orElseGet(() -> {
                    log.warn("No active VK account found for user ID: {}", tgUserId);
                    return null;
                });
    }

    public void setActiveVkAccount(UUID vkAccountId, User user) {
        log.info("Switching active VK account to ID: {} for user ID: {}", vkAccountId, user.getTgUserId());
        vkAccountRepository.deactivateVkAccount(user);
        vkAccountRepository.activateVkAccount(vkAccountId);
        log.info("Activated VK account ID: {} for user ID: {}", vkAccountId, user.getTgUserId());
    }

    public List<VkAccount> getAllUserVkAccounts(Long tgUserId) {
        List<VkAccount> accounts = vkAccountRepository.findAllByUser_TgUserId(tgUserId);
        log.info("Fetched {} VK accounts for user ID: {}", accounts.size(), tgUserId);
        return accounts;
    }

    public Boolean existsVkAccountByUser(User user) {
        boolean exists = vkAccountRepository.existsByUser(user);
        log.info("VK account exists for user ID {}: {}", user.getTgUserId(), exists);
        return exists;
    }

    public Boolean existsVkAccountByUserId(Long tgUserId) {
        boolean exists = vkAccountRepository.existsByUser_TgUserId(tgUserId);
        log.info("Vk account exists for user ID {}: {}", tgUserId, exists);
        return exists;
    }
}
