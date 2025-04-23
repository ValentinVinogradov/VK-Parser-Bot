package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.impl.LoginServiceImpl;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;



@RestController
@RequestMapping("/vk")
public class LoginController {

    private final LoginServiceImpl loginService;

    public LoginController(LoginServiceImpl loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<VkUserInfoDTO>> handleVkAccountAuth(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(name = "device_id") String deviceId,
            @RequestParam(name = "tg_id") Long tgUserId) {

        return loginService.handleVkAccountAuth(tgUserId, code, state, deviceId)
                .doOnSuccess(vkUserInfoDTO -> System.out.println("Successfully handled VK account auth!")) // Логирование успешного выполнения
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    // Логирование ошибки
                    e.printStackTrace();
                    // Возвращаем строку с ошибкой в случае исключения
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null));
                });
    }
}
