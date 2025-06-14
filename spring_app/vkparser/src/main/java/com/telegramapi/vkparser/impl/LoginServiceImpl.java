package com.telegramapi.vkparser.impl;

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

@Service
public class LoginServiceImpl implements LoginService {
    private final UserServiceImpl userService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final TgBotServiceImpl tgBotService;
    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);

    public LoginServiceImpl(UserServiceImpl userService,
                            VkAccountServiceImpl vkAccountService,
                            VkServiceImpl vkService,
                            BlockingServiceImpl blockingService,
                            TgBotServiceImpl tgBotService) {
        this.userService = userService;
        this.vkAccountService = vkAccountService;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.tgBotService = tgBotService;
    }

    //todo разбить по методам
    public Mono<VkUserInfoDTO> handleVkAccountAuth(
            Long tgUserId,
            String code,
            String state,
            String deviceId
    ) {
        //todo ЦЕПОЧКА:
        // 1: Создать пользователя
        // 2: Получить его токены и айди с вк
        // 3: Изменить / Создать вк аккаунт
        // 3.1: Создать его в памяти
        // 3.2: Сделать запросы на получение данных профиля и магазинов
        // 3.3: Сохранить в базе данных
        // 3.4: Активировать аккаунт
        return createTgUser(tgUserId)
            .flatMap(user -> vkService.getUserTokens(code, state, deviceId)
                .flatMap(vkTokenResponseDTO -> {
                    log.debug("Received VK tokens for userId={}, code={}, state={}",
                            user.getTgUserId(), code, state);



//                    return blockingService.fromBlocking(() -> vkAccountService
//                                    .existsVkAccountByVkId(vkUserId))
//                        .flatMap(existsVk -> {
//                            if (existsVk) {
//                                log.info("VK account already exists for VK ID={}, skipping creation",
//                                        vkUserId);
//
//                                return editExistingVkAccount(
//                                        deviceId, vkUserId, accessToken,
//                                        refreshToken, expiresAt, idToken);
//                            }

                    return createNewVkAccount(
                            tgUserId, deviceId, user, vkTokenResponseDTO);
                    })
            .doOnSubscribe(sub -> log.info("Handling VK auth callback started..."))
            .doOnSuccess(sub -> log.info("Handling VK auth callback completed successfully!"))
            .doOnTerminate(() -> log.info("Handling VK auth finished!"))
            .doOnError(e -> log.error("VK auth callback failed", e)));
    }

    private Mono<VkUserInfoDTO> createNewVkAccount(
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
                    Mono<VkUserInfoDTO> vkUserInfoMono = vkService
                            .getUserProfileInfo(accessToken);
                    Mono<List<VkMarket>> vkMarketsMono = vkService
                            .getUserMarkets(vkUserId, accessToken);

                    return getUserInfoAndMarkets(
                            tgUserId, user, vkAccount,
                            vkUserInfoMono, vkMarketsMono);
                });
    }

    private Mono<VkUserInfoDTO> getUserInfoAndMarkets(
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

    private Mono<VkUserInfoDTO> saveAndSyncVkAccount(
            Long tgUserId, User user, VkAccount vkAccount,
            List<VkMarket> vkMarkets, VkUserInfoDTO vkUserInfoDTO) {
        return blockingService.runBlocking(() -> {
                    log.debug("Saving VK account for userId={}",
                            user.getTgUserId());
                    vkAccountService.saveVkAccount(vkAccount);
                    vkAccountService.setActiveVkAccount(
                            vkAccount.getId(), user);
                })
                .then(userService.syncUserMarkets(vkAccount, vkMarkets))
                .then(tgBotService.notifyAuthorizationSuccess(
                        tgUserId, vkUserInfoDTO))
                .thenReturn(vkUserInfoDTO);
    }

    private Mono<VkUserInfoDTO> editExistingVkAccount(
            String deviceId, Long vkUserId, String accessToken,
            String refreshToken, LocalDateTime expiresAt, String idToken) {
        return blockingService.fromBlocking(() -> {
            VkAccount vkAccount = vkAccountService.getVkAccountByVkId(vkUserId);
            // обновление инфы
            vkAccount.setDeviceId(deviceId);
            vkAccount.setAccessToken(accessToken);
            vkAccount.setRefreshToken(refreshToken);
            vkAccount.setExpiresAt(expiresAt);
            vkAccount.setIdToken(idToken);
            // создание dto
            return new VkUserInfoDTO(
                    vkAccount.getFirstName(),
                    vkAccount.getLastName(),
                    vkAccount.getScreenName()
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
                        return blockingService.fromBlocking(() -> userService.getUserByTgId(tgUserId));
                    }
                });
    }
}
