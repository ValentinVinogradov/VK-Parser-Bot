package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.services.LoginService;
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
    public Mono<VkUserInfoDTO> handleVkAccountAuth(Long tgUserId, String code, String state, String deviceId) {
        return blockingService.fromBlocking(() -> userService.existsUserByTgId(tgUserId)) // Проверяем, существует ли пользователь
                .flatMap(exists -> {
                        if(!exists) {
                            User user = userService.createUser(tgUserId);
                            return blockingService.runBlocking(() -> userService.saveUser(user)).then(Mono.just(user));
                        }
                        else {
                            return blockingService.fromBlocking(() -> userService.getUserByTgId(tgUserId));
                        }
                    })
                //todo добавить логику, если вк аккаунт уже существует
                // .flatMap(user -> {
                //     if (user.getVkAccount() != null) {
                //         return Mono.error(new RuntimeException("VK account already exists for this user."));
                //     } else {
                //         return Mono.just(user);
                //     }
                // }) че-то такое
                //todo сделать проверку на существование аккаунта вк в бд, если есть, то обновить токены и синхронизировать рынки
                .flatMap(user -> vkService.getUserTokens(code, state, deviceId) // Получаем токены VK
                        .flatMap(vkTokenResponseDTO -> {
                            LocalDateTime expiresAt = LocalDateTime.now()
                                    .plusSeconds(vkTokenResponseDTO.getExpiresIn());
                            Long vkUserId = vkTokenResponseDTO.getUserId();
                            String accessToken = vkTokenResponseDTO.getAccessToken();
                            String refreshToken = vkTokenResponseDTO.getRefreshToken();
                            String idToken = vkTokenResponseDTO.getIdToken();

                            Mono<VkUserInfoDTO> vkUserInfoMono = vkService.getUserProfileInfo(accessToken);

                            Mono<List<VkMarket>> vkMarketsMono = vkService.getUserMarkets(vkUserId, accessToken);

                            return Mono.zip(vkUserInfoMono, vkMarketsMono)
                                    .flatMap(tuple -> {
                                        VkUserInfoDTO vkUserInfoDTO = tuple.getT1();
                                        List<VkMarket> vkMarkets = tuple.getT2();

                                        VkAccount vkAccount = vkAccountService.createVkAccount(
                                                vkUserId, accessToken, refreshToken, idToken, expiresAt, user
                                        );
                                        vkAccount.setFirstName(vkUserInfoDTO.getFirstName());
                                        vkAccount.setLastName(vkUserInfoDTO.getLastName());
                                        vkAccount.setScreenName(vkUserInfoDTO.getScreenName());

                                        return blockingService.runBlocking(() -> vkAccountService.saveVkAccount(vkAccount))
                                                .then(userService.syncUserMarkets(vkAccount, vkMarkets))
                                                .then(tgBotService.notifyAuthorizationSuccess(tgUserId, vkUserInfoDTO))
                                                .thenReturn(vkUserInfoDTO);
                                    });

//                            Mono<Void> syncMarketsMono = userService
//                                    .syncUserMarkets(user.getTgUserId(), vkUserId, accessToken);

                            // После выполнения запросов
//                            return Mono.when(syncMarketsMono, vkUserInfoMono)
//                                    .then(vkUserInfoMono)
//                                    .flatMap(vkUserInfoDTO -> {
//                                        VkAccount vkAccount = vkAccountService.createVkAccount(
//                                                vkUserId, accessToken, refreshToken, idToken, expiresAt, user
//                                        );
//                                        vkAccount.setFirstName(vkUserInfoDTO.getFirstName());
//                                        vkAccount.setLastName(vkUserInfoDTO.getLastName());
//                                        vkAccount.setScreenName(vkUserInfoDTO.getScreenName());
//
//                                        return blockingService.runBlocking(() -> vkAccountService.saveVkAccount(vkAccount))
//                                                .then(tgBotService.notifyAuthorizationSuccess(tgUserId, vkUserInfoDTO))
//                                                .thenReturn(vkUserInfoDTO);
//                                    });
                        })
                .doOnSubscribe(sub -> System.out.println("Handling vk auth callback started..."))
                .doOnSuccess(sub -> System.out.println("Handling vk auth callback completed successfully!"))
                .doOnTerminate(() -> System.out.println("Handling vk auth finished!")));
    }
}
