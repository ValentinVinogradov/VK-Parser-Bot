package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.impl.LoginServiceImpl;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;



@RestController
@RequestMapping("/vk")
@Validated
public class LoginController {

    private final LoginServiceImpl loginService;

    public LoginController(LoginServiceImpl loginService) {
        this.loginService = loginService;
    }

    //todo разобраться хули Mono (вроде как и так и так можно хз)
    @GetMapping("/callback")
    public Mono<ResponseEntity<VkAccountDTO>> handleVkAccountAuth(
            @RequestParam
            @NotBlank(message = "Code must not be blank")
            String code,

            @NotBlank(message = "State must not be blank")
            @RequestParam
            String state,

            @RequestParam(name = "device_id")
            @NotBlank(message = "Device Id must not be blank")
            String deviceId,

            @RequestParam(name = "tg_id") Long tgUserId) {

        return loginService.handleVkAccountAuth(tgUserId, code, state, deviceId)
                .doOnSuccess(vkUserInfoDTO -> System.out.println("Successfully handled VK account auth!")) // Логирование успешного выполнения
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null));
                });
    }
}
