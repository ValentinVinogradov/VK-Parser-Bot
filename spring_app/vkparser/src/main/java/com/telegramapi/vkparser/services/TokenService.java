package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import reactor.core.publisher.Mono;

public interface TokenService {
    Mono<String> getFreshAccessToken(VkAccountCacheDTO vkDTO, String STATE);

}
