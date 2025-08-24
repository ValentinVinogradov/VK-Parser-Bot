package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.enums.ResponseStatusEnum;
import com.telegramapi.vkparser.impl.LoginServiceImpl;
import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/vk")
@Validated
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private final LoginServiceImpl loginService;

    public LoginController(LoginServiceImpl loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/login")
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

    @DeleteMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(
            @RequestParam(name = "tg_id") Long tgUserId,
            @RequestParam(name = "account_id") UUID accountId
    ) {
        return loginService.logout(tgUserId, accountId)
                .then(Mono.fromSupplier(() -> {
                    Map<String, String> body = Map.of(
                            "status", ResponseStatusEnum.SUCCESS.toString(),
                            "message", "Successfully logged out VK account!"
                    );
                    return ResponseEntity.ok(body);
                }))
                .doOnSuccess(r -> log.info("Successfully logout user VK account!"))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    Map<String, String> errorBody = Map.of(
                            "status", ResponseStatusEnum.ERROR.toString(),
                            "message", e.getMessage()
                    );
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorBody));
                });
    }
}
