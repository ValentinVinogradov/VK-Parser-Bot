package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.services.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Block;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class TokenServiceImpl implements TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final VkAccountServiceImpl vkAccountService;

    public TokenServiceImpl(VkServiceImpl vkService, BlockingServiceImpl blockingService, VkAccountServiceImpl vkAccountService) {
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.vkAccountService = vkAccountService;
    }

    public Mono<String> getFreshAccessToken(VkAccountCacheDTO vkDTO, String STATE) {
        return Mono.defer(() -> {
            if (vkDTO.expiresAt().isBefore(LocalDateTime.now().plusSeconds(30))) {
                log.info("Access token expired or about to expire. Refreshing...");
                return vkService
                        .refreshAccessToken(vkDTO.refreshToken(), STATE, vkDTO.deviceId())
                        .flatMap(refresh -> {
                            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(refresh.expiresIn());

                            //todo
                            blockingService.runBlocking(() ->
                                    vkAccountService.updateVkAccountFields(
                                            vkDTO.id(),
                                            refresh.accessToken(),
                                            refresh.refreshToken(),
                                            newExpiresAt
                                    )
                            );
                            log.info("Access token refreshed successfully for VK account ID: {}", vkDTO.id());
                            return Mono.just(refresh.accessToken());
                        });
            } else {
                return Mono.just(vkDTO.accessToken());
            }
        });
    }
}
