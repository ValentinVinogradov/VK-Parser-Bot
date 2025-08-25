package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.repositories.VkAccountRepository;
import com.telegramapi.vkparser.services.VkAccountService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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
    private final RedisServiceImpl redisService;

    public VkAccountServiceImpl(VkAccountRepository vkAccountRepository, RedisServiceImpl redisService) {
        this.vkAccountRepository = vkAccountRepository;
        this.redisService = redisService;
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

    public void updateVkAccountFields(UUID id,
                                      String accessToken,
                                      String refreshToken,
                                      LocalDateTime expiresAt) {
        vkAccountRepository.updateVkAccountFields(id, accessToken, refreshToken, expiresAt);
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
        log.info("Getting active vk account for user ID: {}", tgUserId);
        return vkAccountRepository.findByUser_TgUserIdAndIsActiveTrue(tgUserId)
                .map(account -> {
                    log.info("Found active VK account for user ID: {}", tgUserId);
                    return account;
                })
                .orElseGet(() -> {
                    log.info("No active VK account found for user ID: {}", tgUserId);
                    return null;
                });
    }

    @Transactional
    public void deleteAccountById(UUID id) {
        vkAccountRepository.deleteById(id);
    }

    @Transactional
    public void setActiveVkAccount(UUID vkAccountId, Long tgUserId) {
        log.info("Switching active VK account to ID: {} for user ID: {}", vkAccountId, tgUserId);
        vkAccountRepository.deactivateVkAccount(tgUserId);
        vkAccountRepository.activateVkAccount(vkAccountId);
        log.info("Activated VK account ID: {} for user ID: {}", vkAccountId, tgUserId);
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

    public UUID getActiveAccountId(Long tgUserId) {
        UUID activeVkAccountId;
        VkAccountCacheDTO cachedVkAccount = redisService
                .getValue(String.format("user:%s:active_vk_account", tgUserId), VkAccountCacheDTO.class);
        log.info("Cached vk account: {}", cachedVkAccount);
        if (cachedVkAccount != null) {
            log.info("Found cache vk account");
            activeVkAccountId = cachedVkAccount.id();
            log.info("Cached vk account id: {}", activeVkAccountId);
        } else {
            log.info("No found cached vk account");
            activeVkAccountId = getActiveAccount(tgUserId).getId();
            log.info("Vk account id from db: {}", activeVkAccountId);
        }

        return activeVkAccountId;
    }

    public VkAccountCacheDTO createVkCacheAccount(Long tgUserId) {
        VkAccountCacheDTO cacheVkAccount = redisService
                .getValue(String.format("user:%s:active_vk_account", tgUserId), VkAccountCacheDTO.class);
        log.info("Cached vk account: {}", cacheVkAccount);
        if (cacheVkAccount == null) {
            log.info("No found cached vk account");
            VkAccount activeVkAccount = getActiveAccount(tgUserId);
            if (activeVkAccount == null) {
                log.warn("No active VK account found for user ID: {}", tgUserId);
                throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
            }
            cacheVkAccount = createAccountCacheDTO(activeVkAccount);
            log.info("Vk account id from db: {}", activeVkAccount.getId());
        }
        return cacheVkAccount;
    }

    public VkAccountCacheDTO createAccountCacheDTO(VkAccount activeVkAccount) {
        return new VkAccountCacheDTO(
                activeVkAccount.getId(),
                activeVkAccount.getAccessToken(),
                activeVkAccount.getRefreshToken(),
                activeVkAccount.getDeviceId(),
                activeVkAccount.getExpiresAt());
    }

    public VkAccountCacheDTO getVkAccountCacheDTO(Long tgUserId, UUID userAccountId) {
        VkAccountCacheDTO cacheVkAccount = redisService
                .getValue(String.format("user:%s:active_vk_account", tgUserId), VkAccountCacheDTO.class);
        if (cacheVkAccount == null) {
            VkAccount vkAccount = getVkAccountById(userAccountId);
            cacheVkAccount = createAccountCacheDTO(vkAccount);
        }
        return cacheVkAccount;
    }

    public List<VkAccountDTO> getVkAccountsFromCache(Long tgUserId) {
        return redisService.getListValue(
                String.format("info:%s:vk_accounts", tgUserId),
                VkAccountDTO.class
        );
    }

    public List<VkAccountDTO> getVkAccountListDTO(Long tgUserId) {
        return getAllUserVkAccounts(tgUserId)
                    .stream()
                    .map(vkAccount -> new VkAccountDTO(
                            vkAccount.getId(),
                            vkAccount.getFirstName(),
                            vkAccount.getLastName(),
                            vkAccount.getScreenName(),
                            vkAccount.getActive()
                    ))
                    .toList();
    }
}
