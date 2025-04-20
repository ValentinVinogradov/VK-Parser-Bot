package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.services.LoginService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class LoginServiceImpl implements LoginService {
    private final UserServiceImpl userService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;

    public LoginServiceImpl(UserServiceImpl userService,
                            VkAccountServiceImpl vkAccountService,
                            VkServiceImpl vkService, BlockingServiceImpl blockingService) {
        this.userService = userService;
        this.vkAccountService = vkAccountService;
        this.vkService = vkService;
        this.blockingService = blockingService;
    }

    public Mono<Void> handleVkAccountAuth(Long tgUserId, String code, String state, String deviceId) {
        return blockingService.fromBlocking(() -> userService.existsUserByTgId(tgUserId)) // Проверяем, существует ли пользователь
                .flatMap(exists -> {
                        if(!exists) {
                            User user = userService.createUser(tgUserId);
                            return blockingService.fromBlocking(() -> userService.saveUser(user));
                        }
                        else {
                            return blockingService.fromBlocking(() -> userService.getUserByTgId(tgUserId));
                        }
                    })
                .flatMap(user -> vkService.getUserTokens(code, state, deviceId) // Получаем токены VK
                        .flatMap(vkTokenResponseDTO -> {
                            LocalDateTime expiresAt = LocalDateTime.now()
                                    .plusSeconds(vkTokenResponseDTO.getExpiresIn());
                            Long vkUserId = vkTokenResponseDTO.getUserId();
                            String accessToken = vkTokenResponseDTO.getAccessToken();
                            String refreshToken = vkTokenResponseDTO.getRefreshToken();

                            VkAccount vkAccount = vkAccountService.createVkAccount(
                                    vkUserId, accessToken, refreshToken, expiresAt, user
                            );

                            Mono<Void> saveVkAccountMono = blockingService.runBlocking(() -> vkAccountService.saveVkAccount(vkAccount));

                            Mono<Void> syncMarketsMono = userService.syncUserMarkets(user.getTgUserId(), vkUserId, accessToken)
                                    .doOnSubscribe(s -> System.out.println("Starting syncUserMarkets..."))
                                    .doOnSuccess(v -> System.out.println("SyncUserMarkets completed successfully!"))
                                    .doOnError(e -> {
                                        System.out.println("Error in syncUserMarkets: " + e.getMessage());
                                        e.printStackTrace();
                                    });

                            return Mono.when(saveVkAccountMono, syncMarketsMono)
                                    .doOnSubscribe(s -> System.out.println("Parallel process started"))
                                    .doOnSuccess(v -> System.out.println("Parallel completed"))
                                    .doOnError(e -> System.out.println("Error in parallel " + e));
                        }))
                .then() // Завершаем процесс
                .doOnSubscribe(sub -> System.out.println("Handling vk auth callback started..."))
                .doOnSuccess(sub -> System.out.println("Handling vk auth callback completed successfully!"))
                .doOnTerminate(() -> System.out.println("Handling vk auth finished!"));
    }

//    public Mono<Void> handleVkAccountAuth(Long tgUserId, String code, String state, String deviceId) {
//        return userService.existsUserByTgId(tgUserId)
//                .flatMap(exists -> {
//                    if (!exists) {
//                        User user = userService.createUser(tgUserId);
//                        return userService.saveUser(user);
//                    } else {
//                        return userService.getUserByTgId(tgUserId)
//                                .flatMap(user -> vkService.getUserTokens(code, state, deviceId)
//                                        .flatMap(vkTokenResponseDTO -> {
//                                            LocalDateTime expiresAt = LocalDateTime
//                                                    .now()
//                                                    .plusSeconds(vkTokenResponseDTO.getExpiresIn());
//                                            Long vkUserId = vkTokenResponseDTO.getUserId();
//                                            String accessToken = vkTokenResponseDTO.getAccessToken();
//                                            String refreshToken = vkTokenResponseDTO.getRefreshToken();
//                                            // todo добавить потом реализацию обновления access token
//                                            VkAccount vkAccount = vkAccountService.createVkAccount(
//                                                    vkUserId,
//                                                    accessToken, refreshToken,
//                                                    expiresAt, user);
//                                            vkAccountService.saveVkAccount(vkAccount);
//
//                                            //todo дописать
//                                            return userService.syncUserMarkets(tgUserId, vkUserId, accessToken);
//                                        }));
//                    }
//                })
//                .then()
//                .doOnTerminate(() -> System.out.println("Handling vk auth finished!"))
//                .doOnSubscribe(sub -> System.out.println("Handling vk auth callback started..."))
//                .doOnSuccess(sub -> System.out.println("Handling vk auth callback completed successfully!"));
//
//    }
}
