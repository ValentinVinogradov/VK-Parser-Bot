package com.telegramapi.vkparser.impl;


import com.telegramapi.vkparser.dto.*;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.UserRepository;
import com.telegramapi.vkparser.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private final String STATE = System.getenv("VK_STATE");
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserRepository userRepository;
    private final UserMarketServiceImpl userMarketService;
    private final BlockingServiceImpl blockingService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkMarketServiceImpl vkMarketService;
    private final VkServiceImpl vkService;
    private final RedisServiceImpl redisService;
    private final TokenServiceImpl tokenService;

    public UserServiceImpl(UserMarketServiceImpl userMarketService,
                           BlockingServiceImpl blockingService,
                           UserRepository userRepository,
                           VkAccountServiceImpl vkAccountService,
                           VkMarketServiceImpl vkMarketService,
                           VkServiceImpl vkService, 
                           RedisServiceImpl redisService,
                           TokenServiceImpl tokenService) {
        this.userMarketService = userMarketService;
        this.blockingService = blockingService;
        this.userRepository = userRepository;
        this.vkAccountService = vkAccountService;
        this.vkMarketService = vkMarketService;
        this.vkService = vkService;
        this.redisService = redisService;
        this.tokenService = tokenService;
    }

    @Override
    public User getUserByTgId(Long tgUserId) {
        log.debug("Fetching user by Telegram ID: {}", tgUserId);
        return userRepository.findByTgUserId(tgUserId);
    }

    @Override
    public Mono<Void> syncUserMarkets(VkAccount vkAccount, List<VkMarket> vkMarkets) {
        log.info("Starting sync of {} VK markets for VK account ID: {}", vkMarkets.size(), vkAccount.getId());

        return Flux.fromIterable(vkMarkets)
                .flatMap(vkMarket ->
                        blockingService.fromBlocking(() -> vkMarketService.existsByVkId(vkMarket.getMarketVkId()))
                                .flatMap(exists -> {
                                    if (exists) {
                                        VkMarket vkMarketToSync = vkMarketService.getMarketById(vkMarket.getMarketVkId());
                                        return blockingService.runBlocking(() -> createAndSaveUserMarket(vkAccount, vkMarketToSync));

                                    } else {
                                        return blockingService.runBlocking(() -> {
                                            vkMarketService.saveVkMarket(vkMarket);
                                            createAndSaveUserMarket(vkAccount, vkMarket);
                                        });
                                    }
                                })
                )
                .doOnSubscribe(sub -> log.info("User market synchronization initiated for VK account ID: {}", vkAccount.getId()))
                .doOnError(e -> log.error("Error during user market synchronization for VK account ID: {}", vkAccount.getId(), e))
                .then()
                .doOnSuccess(unused -> log.info("User market synchronization completed for VK account ID: {}", vkAccount.getId()))
                .doOnTerminate(() -> log.info("User markets synchronization finished."))
                .then();
    }

    private void createAndSaveUserMarket(VkAccount vkAccount, VkMarket vkMarket) {
        UserMarket userMarket = userMarketService
                .createUserMarket(vkAccount, vkMarket);
        userMarketService.saveUserMarket(userMarket);
    }

    @Override
    public Boolean existsUserByTgId(Long tgUserId) {
        boolean exists = userRepository.existsByTgUserId(tgUserId);
        log.debug("Checking existence of user by Telegram ID {}: {}", tgUserId, exists);
        return exists;
    }

    public List<VkAccountDTO> getAllUserVkAccounts(Long tgUserId) {
        log.info("Fetching all VK accounts for user ID: {}", tgUserId);
        List<VkAccountDTO> vkAccounts = vkAccountService.getVkAccountsFromCache(tgUserId);

        log.info("Cached vk accounts: {}", vkAccounts);
        if (vkAccounts == null || vkAccounts.isEmpty()) {
            log.info("No found cached vk accounts");
            vkAccounts = vkAccountService.getVkAccountListDTO(tgUserId);
            log.info("Vk accounts from db: {}", vkAccounts);
            if (!vkAccounts.isEmpty()) {
                redisService.setValue(String.format("info:%s:vk_accounts", tgUserId), vkAccounts);
                log.debug("Vk accounts added to cache");
            }
        }
        return vkAccounts;
    }


    public List<VkMarketDTO> getAllUserMarkets(Long tgUserId) {
        log.info("Fetching VK markets for user ID: {}", tgUserId);
        List<VkMarketDTO> vkMarkets = vkMarketService.getVkMarketsFromCache(tgUserId);
        log.info("Cached vk markets: {}", vkMarkets);
        if (vkMarkets == null || vkMarkets.isEmpty()) {
            log.info("No found cached vk markets");
            UUID activeAccountId = vkAccountService.getActiveAccountId(tgUserId);
            if (activeAccountId != null) {
                vkMarkets = vkMarketService.getVkMarketListDTO(activeAccountId);
                log.info("Vk markets from db: {}", vkMarkets);
                if (!vkMarkets.isEmpty()) {
                    redisService.setValue(String.format("info:%s:vk_markets", tgUserId), vkMarkets);
                    log.info("Vk accounts added to cache");
                } 
            } else {
                log.warn("No active VK account found for user ID to get user markets: {}", tgUserId);
                throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
            }
        }
        return vkMarkets;
    }




    public Mono<List<VkMarket>> getVkMarkets(VkAccount vkAccount) {
        log.info("Fetching vk markets from VK for VK ID: {}", vkAccount.getVkUserId());
        Long vkUserId = vkAccount.getVkUserId();
        VkAccountCacheDTO vkDTO = new VkAccountCacheDTO(
                vkAccount.getId(),
                vkAccount.getAccessToken(),
                vkAccount.getRefreshToken(),
                vkAccount.getDeviceId(),
                vkAccount.getExpiresAt());
        return tokenService.getFreshAccessToken(vkDTO, STATE)
            .flatMap(accessToken -> {
                log.info("Refreshing token for fetch vk markets completed.");
                return vkService.getUserMarkets(vkUserId, accessToken);
            });
    }

    public Mono<VkUserInfoDTO> getUserInfo(VkAccount vkAccount) {
        log.info("Fetching user profile info from VK.");
        VkAccountCacheDTO vkDTO = new VkAccountCacheDTO(
                vkAccount.getId(),
                vkAccount.getAccessToken(),
                vkAccount.getRefreshToken(),
                vkAccount.getDeviceId(),
                vkAccount.getExpiresAt());
        return tokenService.getFreshAccessToken(vkDTO, STATE)
            .flatMap(accessToken -> {
                log.info("Refreshing token for user info completed.");
                return vkService.getUserProfileInfo(accessToken);
            });
    }


    @Override
    public User createUser(Long tgUserId) {
        log.info("Creating new user with Telegram ID: {}", tgUserId);
        User user = new User();
        user.setTgUserId(tgUserId);
        return user;
    }

    @Override
    public void saveUser(User user) {
        log.info("Saving user with Telegram ID: {}", user.getTgUserId());
        userRepository.save(user);
    }

    @Override
    public FullUserInfoDTO getFullUserInfo(Long tgUserId) {
        log.info("Fetching full user info for Telegram ID: {}", tgUserId);
        
        
        List<VkAccountDTO> vkAccounts = getAllUserVkAccounts(tgUserId);
        List<VkMarketDTO> userMarkets = getAllUserMarkets(tgUserId);

        return new FullUserInfoDTO(vkAccounts, userMarkets);
    }

    public Boolean checkActiveVkAccount(Long tgUserId) {
        log.debug("Checking active account for Telegram user ID: {}", tgUserId);
        VkAccountCacheDTO cachedVkAccount = redisService
                .getValue(String.format("user:%s:active_vk_account", tgUserId), VkAccountCacheDTO.class);
        if (cachedVkAccount != null) {
            return true;
        }
        VkAccount vkAccount = vkAccountService.getActiveAccount(tgUserId);
        if (vkAccount != null) {
            VkAccountCacheDTO vkAccountCacheDTO = new VkAccountCacheDTO(
                vkAccount.getId(),
                vkAccount.getAccessToken(),
                vkAccount.getRefreshToken(),
                vkAccount.getDeviceId(),
                vkAccount.getExpiresAt());
            redisService.setValue(String.format("user:%s:active_vk_account", tgUserId), vkAccountCacheDTO);
            return true;
        }
        log.warn("No active VK account found for user ID: {}", tgUserId);
        return false;
    }


    public Boolean checkUserActiveMarket(Long tgUserId) {
        log.debug("Checking active market for user ID: {}", tgUserId);
        VkMarketCacheDTO cachedVkMarket = redisService
                .getValue(String.format("user:%s:active_vk_market", tgUserId), VkMarketCacheDTO.class);
        if (cachedVkMarket != null) {
            return true;
        }

        UUID activeVkAccountId = vkAccountService.getActiveAccountId(tgUserId);
        if (activeVkAccountId != null) {
            UserMarket userMarket = userMarketService.getActiveUserMarket(activeVkAccountId);
            if (userMarket != null) {
                VkMarket vkMarket = userMarket.getVkMarket();
                VkMarketCacheDTO vkMarketCacheDTO = new VkMarketCacheDTO(
                    userMarket.getId(), vkMarket.getMarketVkId()
                );
                redisService.setValue(String.format("user:%s:active_vk_market", tgUserId), vkMarketCacheDTO);
                return true;
            }
            return false;
        } else {
            log.warn("No active VK account found for user ID to check active market: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }

    public void updateActiveMarket(Long tgUserId, UUID userMarketId) {
        log.info("Updating active market for user ID: {} to market ID: {}", tgUserId, userMarketId);
        UUID activeVkAccountId = vkAccountService.getActiveAccountId(tgUserId);
        if (activeVkAccountId != null) {
            userMarketService.setActiveUserMarket(activeVkAccountId, userMarketId);
            UserMarket activeUserMarket = userMarketService.getActiveUserMarket(activeVkAccountId);
            VkMarketCacheDTO vkMarketCacheDTO = new VkMarketCacheDTO(
                userMarketId, activeUserMarket.getVkMarket().getMarketVkId()
            );
            redisService.setValue(String.format("user:%s:active_vk_market", tgUserId), vkMarketCacheDTO);
            redisService.deleteKey(String.format("fsm:%s:data", tgUserId));
            redisService.deleteAllProductPages(tgUserId);
            log.info("New active vk market insert in cache");
        } else {
            log.warn("No active VK account found for user ID to update active market: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }

    public void updateActiveAccount(Long tgUserId, UUID userAccountId) {
        log.info("Updating active account for user ID: {} to account ID: {}", tgUserId, userAccountId);
        if (userAccountId != null) {
            vkAccountService.setActiveVkAccount(userAccountId, tgUserId);
            VkAccount vkAccount = vkAccountService.getActiveAccount(tgUserId);
            VkAccountCacheDTO vkAccountCacheDTO = new VkAccountCacheDTO(
                vkAccount.getId(),
                vkAccount.getAccessToken(),
                vkAccount.getRefreshToken(),
                vkAccount.getDeviceId(),
                vkAccount.getExpiresAt());
            redisService.setValue(String.format("user:%s:active_vk_account", tgUserId), vkAccountCacheDTO);
            redisService.deleteKey(String.format("user:%s:active_vk_market", tgUserId));
            redisService.deleteKey(String.format("fsm:%s:data", tgUserId));
            redisService.deleteAllProductPages(tgUserId);
            redisService.deleteInfoCache(tgUserId);
        } else {
            log.warn("No active VK account found for user ID to update active account: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }

}
