package com.telegramapi.vkparser.services;

import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TgBotService {
    Mono<String> notifyAuthorizationSuccess(Long tgUserId, VkUserInfoDTO userInfoDTO);
}
