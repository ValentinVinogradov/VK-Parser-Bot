package com.telegramapi.vkparser.impl;



import com.telegramapi.vkparser.dto.FullUserInfoDTO;
import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkMarketCacheDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkMarketDTO;
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

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMarketServiceImpl userMarketService;
    private final BlockingServiceImpl blockingService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkMarketServiceImpl vkMarketService;
    private final VkServiceImpl vkService;
    private final RedisServiceImpl redisService;

    public UserServiceImpl(UserMarketServiceImpl userMarketService,
                           BlockingServiceImpl blockingService,
                           UserRepository userRepository,
                           VkAccountServiceImpl vkAccountService,
                           VkMarketServiceImpl vkMarketService,
                           VkServiceImpl vkService, 
                           RedisServiceImpl redisService) {
        this.userMarketService = userMarketService;
        this.blockingService = blockingService;
        this.userRepository = userRepository;
        this.vkAccountService = vkAccountService;
        this.vkMarketService = vkMarketService;
        this.vkService = vkService;
        this.redisService = redisService;
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
                        blockingService.runBlocking(() -> vkMarketService.saveVkMarket(vkMarket))
                                .thenReturn(vkMarket)
                                .flatMap(savedVkMarket -> {
                                    UserMarket userMarket = userMarketService.createUserMarket(vkAccount, savedVkMarket);
                                    return blockingService.runBlocking(() -> userMarketService.saveUserMarket(userMarket));
                                })
                )
                .doOnSubscribe(sub -> log.info("User market synchronization initiated for VK account ID: {}", vkAccount.getId()))
                .doOnError(e -> log.error("Error during user market synchronization for VK account ID: {}", vkAccount.getId(), e))
                .then()
                .doOnSuccess(unused -> log.info("User market synchronization completed for VK account ID: {}", vkAccount.getId()))
                .doOnTerminate(() -> log.info("User markets synchronization finished."))
                .then();
    }

    @Override
    public Boolean existsUserByTgId(Long tgUserId) {
        boolean exists = userRepository.existsByTgUserId(tgUserId);
        log.debug("Checking existence of user by Telegram ID {}: {}", tgUserId, exists);
        return exists;
    }

    public List<VkAccountDTO> getAllUserVkAccounts(Long tgUserId) {
        log.info("Fetching all VK accounts for user ID: {}", tgUserId);
        List<VkAccountDTO> vkAccounts = redisService.getListValue(
                String.format("info:%s:vk_accounts", tgUserId),
                VkAccountDTO.class
        );

        log.info("Cached vk accounts: {}", vkAccounts);
        if (vkAccounts == null || vkAccounts.isEmpty()) {
            log.info("No found cached vk accounts");
            vkAccounts = vkAccountService
                .getAllUserVkAccounts(tgUserId)
                .stream()
                .map(vkAccount -> new VkAccountDTO(
                        vkAccount.getId(),
                        vkAccount.getFirstName(),
                        vkAccount.getLastName(),
                        vkAccount.getScreenName(),
                        vkAccount.getActive()
                ))
                .toList();
            log.info("Vk accounts from db: {}", vkAccounts);

                //todo надо ли при подгрузке профиля (акков) заново просчитывать activeAccount
                // VkAccountCacheDTO cachedVkAccount = (VkAccountCacheDTO) redisService.getValue(String.format("user:%s:active_vk_account", tgUserId));
                // if (cachedVkAccount != null)
            if (!vkAccounts.isEmpty()) {
                redisService.setValue(String.format("info:%s:vk_accounts", tgUserId), vkAccounts);
                log.debug("Vk accounts added to cache");
            }
        }
        return vkAccounts;
    }

    public List<VkMarketDTO> getAllUserMarkets(Long tgUserId) {
        log.info("Fetching VK markets for user ID: {}", tgUserId);
        List<VkMarketDTO> vkMarkets = redisService.
            getListValue(String.format("info:%s:vk_markets", tgUserId), VkMarketDTO.class);
        log.info("Cached vk markets: {}", vkMarkets);
        if (vkMarkets == null || vkMarkets.isEmpty()) {
            log.info("No found cached vk markets");
            UUID activeAccountId;
            VkAccountCacheDTO cachedVkAccount = redisService
                .getValue(String.format("user:%s:active_vk_account", tgUserId), VkAccountCacheDTO.class);
            log.info("Cached vk account: {}", cachedVkAccount);
            if (cachedVkAccount != null) {
                log.info("Found cache vk account");
                activeAccountId = cachedVkAccount.id();
                log.info("Cached vk account id: {}", activeAccountId);
            } else {
                log.info("No found cached vk account");
                activeAccountId = vkAccountService.getActiveAccount(tgUserId).getId();
                log.info("Vk account id from db: {}", activeAccountId);
            }
            if (activeAccountId != null) {
                List<UserMarket> userMarkets = userMarketService.getAllUserMarkets(activeAccountId);
                vkMarkets = userMarkets
                        .stream()
                        .map(userMarket -> new VkMarketDTO(
                                userMarket.getId(),
                                userMarket.getVkMarket().getMarketName(),
                                userMarket.getVkMarket().getMembersCount(),
                                userMarket.getActive()
                        ))
                        .toList();
                log.info("Vk markets from db: {}", vkMarkets);
                if (!vkMarkets.isEmpty()) {
                    redisService.setValue(String.format("info:%s:vk_markets", tgUserId), vkMarkets);
                    log.info("Vk accounts added to cache");
                } 
            } else {
                log.warn("No active VK account found for user ID: {}", tgUserId);
                throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
            }
        }
        return vkMarkets;
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
//        User user = getUserByTgId(tgUserId);
        
        
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
                vkAccount.getId(), vkAccount.getAccessToken());
            redisService.setValue(String.format("user:%s:active_vk_account", tgUserId), vkAccountCacheDTO);
            return true;
        }
        return false;
    }

    //todo можно акк с кеша получить, передав тг айди
    public Boolean checkUserActiveMarket(Long tgUserId) {
        log.debug("Checking active market for user ID: {}", tgUserId);
        VkMarketCacheDTO cachedVkMarket = redisService
                .getValue(String.format("user:%s:active_vk_market", tgUserId), VkMarketCacheDTO.class);
        if (cachedVkMarket != null) {
            return true;
        }

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
            activeVkAccountId = vkAccountService.getActiveAccount(tgUserId).getId();
            log.info("Vk account id from db: {}", activeVkAccountId);
        }
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
            log.warn("No active VK account found for user ID: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }

    // public Boolean checkUserActiveMarket(Long tgUserId) {
    //     log.debug("Checking active market for Telegram user ID: {}", tgUserId);
    //     VkAccount activeAccount = vkAccountService.getActiveAccount(tgUserId);
    //     if (activeAccount == null) {
    //         log.warn("No active VK market found for user ID: {}", tgUserId);
    //         return false;
    //     }
    //     UUID vkAccountId = activeAccount.getId();
    //     return userMarketService.getActiveUserMarket(vkAccountId) != null;
    // }

    public void updateActiveMarket(Long tgUserId, UUID userMarketId) {
        log.info("Updating active market for user ID: {} to market ID: {}", tgUserId, userMarketId);
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
            activeVkAccountId = vkAccountService.getActiveAccount(tgUserId).getId();
            log.info("Vk account id from db: {}", activeVkAccountId);
        }
        if (activeVkAccountId != null) {
            userMarketService.setActiveUserMarket(activeVkAccountId, userMarketId);
            UserMarket activeUserMarket = userMarketService.getActiveUserMarket(activeVkAccountId);
            VkMarketCacheDTO vkMarketCacheDTO = new VkMarketCacheDTO(
                userMarketId, activeUserMarket.getVkMarket().getMarketVkId()
            );
            redisService.setValue(String.format("user:%s:active_vk_market", tgUserId), vkMarketCacheDTO);
            log.info("New active vk market insert in cache");
        } else {
            log.warn("No active VK account found for user ID: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }

    //todo
    public void logoutVkAccount(UUID vkAccountId) {
        VkAccount vkAccount = vkAccountService.getVkAccountById(vkAccountId);
        String accessToken = vkAccount.getAccessToken();
        vkService.logout(accessToken);
    }
}
