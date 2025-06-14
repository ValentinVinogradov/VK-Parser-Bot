package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.VkAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Block;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class TokenServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final VkAccountServiceImpl vkAccountService;

    public TokenServiceImpl(VkServiceImpl vkService, BlockingServiceImpl blockingService, VkAccountServiceImpl vkAccountService) {
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.vkAccountService = vkAccountService;
    }

    public Mono<String> getFreshAccessToken(VkAccount vkAccount, String STATE) {
        return Mono.defer(() -> {
            if (vkAccount.getExpiresAt().isBefore(LocalDateTime.now().plusSeconds(30))) {
                log.info("Access token expired or about to expire. Refreshing...");
                return vkService
                        .refreshAccessToken(vkAccount.getRefreshToken(), STATE, vkAccount.getDeviceId())
                        .flatMap(refresh -> {
                            vkAccount.setAccessToken(refresh.accessToken());
                            vkAccount.setRefreshToken(refresh.refreshToken());
                            vkAccount.setExpiresAt(LocalDateTime.now().plusSeconds(refresh.expiresIn()));
                            blockingService.runBlocking(() -> vkAccountService.saveVkAccount(vkAccount));
                            log.info("Access token refreshed successfully for VK account ID: {}", vkAccount.getId());
                            return blockingService.fromBlocking(vkAccount::getAccessToken);
                        });
            } else {
                return Mono.just(vkAccount.getAccessToken());
            }
        });
    }
}
