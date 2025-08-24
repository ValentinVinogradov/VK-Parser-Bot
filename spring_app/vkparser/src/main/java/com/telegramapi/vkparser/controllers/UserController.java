package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.FullUserInfoDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkMarketDTO;
import com.telegramapi.vkparser.enums.ResponseStatusEnum;
import com.telegramapi.vkparser.impl.UserServiceImpl;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkMarket;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {
    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public ResponseEntity<FullUserInfoDTO> getFullUserInfo(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.getFullUserInfo(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/vk-accounts")
    public ResponseEntity<List<VkAccountDTO>> getAllVkAccounts(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId
    ) {
        try {
            return ResponseEntity.ok(userService.getAllUserVkAccounts(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/markets")
    public ResponseEntity<List<VkMarketDTO>> getAllMarkets(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId
    ) {
        try {
            return ResponseEntity.ok(userService.getAllUserMarkets(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


    @PostMapping("/create-user")
    public ResponseEntity<Map<String, String>> createUser(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId) {
        try {
            if (!userService.existsUserByTgId(tgUserId)) {
                User user = userService.createUser(tgUserId);
                userService.saveUser(user);
            }
            Map<String, String> body = Map.of(
                    "status", ResponseStatusEnum.SUCCESS.toString(),
                    "message", "Successfully created user!"
            );
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, String> body = Map.of(
                    "status", ResponseStatusEnum.ERROR.toString(),
                    "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/check-active-account")
    public ResponseEntity<Boolean> checkUserActiveAccount(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.checkActiveVkAccount(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/check-active-market")
    public ResponseEntity<Boolean> checkUserActiveMarket(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.checkUserActiveMarket(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PatchMapping("/update-active-market")
    public ResponseEntity<Map<String, String>> updateActiveMarket(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId,

            @RequestParam(name = "market_id")
            @NotBlank(message = "Market ID must not be blank")
            UUID marketId) {
        try {
            userService.updateActiveMarket(tgUserId, marketId);

            Map<String, String> body = Map.of(
                    "status", ResponseStatusEnum.SUCCESS.toString(),
                    "message", "Successfully updated active market!"
            );
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, String> body = Map.of(
                    "status", ResponseStatusEnum.ERROR.toString(),
                    "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(body);
        }
    }

    @PatchMapping("/update-active-account")
    public ResponseEntity<Map<String, String>> updateActiveAccount(
            @RequestParam(name = "tg_id")
            @NotBlank(message = "TG ID must not be blank")
            Long tgUserId,

            @RequestParam(name = "account_id")
            @NotBlank(message = "Account ID must not be blank")
            UUID accountId) {
        try {
            userService.updateActiveAccount(tgUserId, accountId);
            Map<String, String> body = Map.of(
                    "status", ResponseStatusEnum.SUCCESS.toString(),
                    "message", "Successfully updated active account!"
            );
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, String> body = Map.of(
                    "status", ResponseStatusEnum.ERROR.toString(),
                    "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(body);
        }
    }

}
