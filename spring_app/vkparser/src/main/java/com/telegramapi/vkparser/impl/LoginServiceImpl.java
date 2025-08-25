package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.services.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService {
    private final String STATE = System.getenv("VK_STATE");
    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

    private final UserServiceImpl userService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final TgBotServiceImpl tgBotService;
    private final RedisServiceImpl redisService;
    private final TokenServiceImpl tokenService;

    public LoginServiceImpl(UserServiceImpl userService,
                            VkAccountServiceImpl vkAccountService,
                            VkServiceImpl vkService,
                            BlockingServiceImpl blockingService,
                            TgBotServiceImpl tgBotService,
                            RedisServiceImpl redisService,
                            TokenServiceImpl tokenService) {
        this.userService = userService;
        this.vkAccountService = vkAccountService;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.tgBotService = tgBotService;
        this.redisService = redisService;
        this.tokenService = tokenService;
    }


    public Mono<VkAccountDTO> handleVkAccountAuth(
            Long tgUserId,
            String code,
            String state,
            String deviceId
    ) {
        return createTgUser(tgUserId)
                .flatMap(user -> vkService.getUserTokens(code, state, deviceId)
                        .flatMap(vkTokenResponseDTO -> {
                            log.debug("Received VK tokens for userId={}, code={}, state={}",
                                    user.getTgUserId(), code, state);

                            Long vkUserId = vkTokenResponseDTO.userId();
                            String accessToken = vkTokenResponseDTO.accessToken();
                            String refreshToken = vkTokenResponseDTO.refreshToken();
                            LocalDateTime expiresAt = LocalDateTime.now()
                                    .plusSeconds(vkTokenResponseDTO.expiresIn());

                            redisService.deleteInfoCache(tgUserId);

                            return blockingService.fromBlocking(() ->
                                            vkAccountService.existsVkAccountByVkId(vkUserId))
                                    .flatMap(existsVk -> {
                                        if (existsVk) {
                                            log.info("VK account already exists for VK ID={}, skipping creation",
                                                    vkUserId);

                                            return editAndGetExistingVkAccount(
                                                    user.getTgUserId(),
                                                    deviceId,
                                                    vkUserId,
                                                    accessToken,
                                                    refreshToken,
                                                    expiresAt
                                            );
                                        }

                                        return createNewVkAccount(
                                                tgUserId,
                                                deviceId,
                                                user,
                                                vkTokenResponseDTO
                                        );
                                    });
                        }))
                .doOnSubscribe(sub -> log.info("Handling VK auth callback started..."))
                .doOnSuccess(sub -> log.info("Handling VK auth callback completed successfully!"))
                .doOnTerminate(() -> log.info("Handling VK auth finished!"))
                .doOnError(e -> log.error("VK auth callback failed", e));
    }

    private Mono<VkAccountDTO> createNewVkAccount(
            Long tgUserId, String deviceId, User user,
            VkTokenResponseDTO vkTokenResponseDTO) {

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(vkTokenResponseDTO.expiresIn());
        Long vkUserId = vkTokenResponseDTO.userId();
        String accessToken = vkTokenResponseDTO.accessToken();
        String refreshToken = vkTokenResponseDTO.refreshToken();
        String idToken = vkTokenResponseDTO.idToken();

        return blockingService.fromBlocking(() -> {
                    log.info("Creating new VK account for userId={}, vkUserId={}",
                            user.getTgUserId(),
                            vkUserId);
                    return vkAccountService.createVkAccount(
                            vkUserId, accessToken, refreshToken,
                            idToken, deviceId, expiresAt, user
                    );
                })
                .flatMap(vkAccount -> {
                    Mono<VkUserInfoDTO> vkUserInfoMono = userService
                            .getUserInfo(vkAccount);
                    Mono<List<VkMarket>> vkMarketsMono = userService
                            .getVkMarkets(vkAccount);

                    return getUserInfoAndMarkets(
                            tgUserId, user, vkAccount,
                            vkUserInfoMono, vkMarketsMono);
                });
    }

    private Mono<VkAccountDTO> getUserInfoAndMarkets(
            Long tgUserId, User user, VkAccount vkAccount,
            Mono<VkUserInfoDTO> vkUserInfoMono, Mono<List<VkMarket>> vkMarketsMono) {
        return Mono.zip(vkUserInfoMono, vkMarketsMono)
                .flatMap(tuple -> {
                    VkUserInfoDTO vkUserInfoDTO = tuple.getT1();
                    List<VkMarket> vkMarkets = tuple.getT2();

                    log.info("VK profile info received: {} {}, screenName={}",
                            vkUserInfoDTO.firstName(),
                            vkUserInfoDTO.lastName(),
                            vkUserInfoDTO.screenName());
                    log.debug("User has {} VK markets", vkMarkets.size());

                    vkAccount.setFirstName(vkUserInfoDTO.firstName());
                    vkAccount.setLastName(vkUserInfoDTO.lastName());
                    vkAccount.setScreenName(vkUserInfoDTO.screenName());

                    return saveAndSyncVkAccount(
                            tgUserId, user, vkAccount,
                            vkMarkets, vkUserInfoDTO);
                });
    }

    private Mono<VkAccountDTO> saveAndSyncVkAccount(
        Long tgUserId, User user, VkAccount vkAccount,
        List<VkMarket> vkMarkets, VkUserInfoDTO vkUserInfoDTO) {
    
        return blockingService.fromBlocking(() -> {
                log.debug("Saving VK account for userId={}", user.getTgUserId());
                vkAccountService.saveVkAccount(vkAccount);
                vkAccountService.setActiveVkAccount(vkAccount.getId(), user.getTgUserId());
                vkAccount.setActive(true);
                VkAccountCacheDTO vkAccountCacheDTO = new VkAccountCacheDTO(
                        vkAccount.getId(), vkAccount.getAccessToken(),
                        vkAccount.getRefreshToken(), vkAccount.getDeviceId(),
                        vkAccount.getExpiresAt()
                );
                redisService.setValue(String.format("user:%s:active_vk_account", tgUserId), vkAccountCacheDTO);


                return new VkAccountDTO(
                        vkAccount.getId(),
                        vkAccount.getFirstName(),
                        vkAccount.getLastName(),
                        vkAccount.getScreenName(),
                        vkAccount.getActive()
                );

                })
                .flatMap(vkAccountDTO ->
                        userService.syncUserMarkets(vkAccount, vkMarkets)
                                .then(tgBotService.notifyAuthorizationSuccess(tgUserId, vkUserInfoDTO))
                                .thenReturn(vkAccountDTO)
                );
}


     private Mono<VkAccountDTO> editAndGetExistingVkAccount(
             Long tgUserId, String deviceId, Long vkUserId, String accessToken,
             String refreshToken, LocalDateTime expiresAt) {
         return blockingService.fromBlocking(() -> {
             VkAccount vkAccount = vkAccountService.getVkAccountByVkId(vkUserId);
             vkAccount.setDeviceId(deviceId);
             vkAccount.setAccessToken(accessToken);
             vkAccount.setRefreshToken(refreshToken);
             vkAccount.setExpiresAt(expiresAt);

             vkAccountService.setActiveVkAccount(vkAccount.getId(), tgUserId);

             vkAccountService.saveVkAccount(vkAccount);

             VkAccountCacheDTO vkAccountCacheDTO = vkAccountService.createAccountCacheDTO(vkAccount);

             redisService.setValue(String.format("user:%s:active_vk_account", tgUserId), vkAccountCacheDTO);

             // создание dto
             return new VkAccountDTO(
                     vkAccount.getId(),
                     vkAccount.getFirstName(),
                     vkAccount.getLastName(),
                     vkAccount.getScreenName(),
                     vkAccount.getActive()
             );
         });
     }

    private Mono<User> createTgUser(Long tgUserId) {
        return blockingService.fromBlocking(() -> {
                    log.debug("Checking if user with tgUserId={} exists", tgUserId);
                    return userService.existsUserByTgId(tgUserId);
                })
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("User doesn't exist, creating new user with tgUserId={}", tgUserId);
                        User user = userService.createUser(tgUserId);
                        return blockingService.runBlocking(() -> {
                            log.debug("Saving new user with tgUserId={}", tgUserId);
                            userService.saveUser(user);
                        }).then(Mono.just(user));
                    } else {
                        log.info("User exists, fetching user with tgUserId={}", tgUserId);
                        return blockingService.fromBlocking(() ->
                                userService.getUserByTgId(tgUserId));
                    }
                });
    }


    public Mono<Void> logout(Long tgUserId, UUID userAccountId) {
        log.info("Deleting account for user ID: {} to account ID: {}", tgUserId, userAccountId);
        if (userAccountId != null) {
            VkAccountCacheDTO cacheVkAccount = vkAccountService
                    .getVkAccountCacheDTO(tgUserId, userAccountId);

            if (userAccountId.equals(cacheVkAccount.id())) {
                log.info("Clearing cache about active vk account and vk markets...");
                redisService.deleteInfoCache(tgUserId);
            }

            return blockingService.runBlocking(() -> {
                        vkAccountService.deleteAccountById(userAccountId);
                        log.info("Account deleted locally: {}", userAccountId);
                        redisService.deleteKey(String.format("info:%s:vk_accounts", tgUserId));
                    })
                    .then(tokenService.getFreshAccessToken(cacheVkAccount, STATE))
                    .flatMap(accessToken -> {
                        log.info("Refreshing token for user info completed.");
                        return vkService.logout(accessToken);
                    });
        } else {
            log.warn("No VK account found for user ID: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }
}
