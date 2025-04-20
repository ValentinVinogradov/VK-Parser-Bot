package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.impl.LoginServiceImpl;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/vk")
public class LoginController {

    private final LoginServiceImpl loginService;

    public LoginController(LoginServiceImpl loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<String>> handleVkAccountAuth(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(name = "device_id") String deviceId,
            @RequestParam(name = "tg_id") Long tgUserId) {

        return loginService.handleVkAccountAuth(tgUserId, code, state, deviceId)
                .thenReturn(ResponseEntity.ok("Successfully worked controller!"))
                .onErrorResume(e -> {
                    // Можно залоггировать ошибку
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
