package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.FullUserInfoDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkMarketDTO;
import com.telegramapi.vkparser.impl.UserServiceImpl;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkMarket;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public ResponseEntity<FullUserInfoDTO> getFullUserInfo(
            @RequestParam(name = "tg_id") Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.getFullUserInfo(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/vk-accounts")
    public ResponseEntity<List<VkAccountDTO>> getAllVkAccounts(
            @RequestParam(name = "tg_id") Long tgUserId
    ) {
        try {
            return ResponseEntity.ok(userService.getAllUserVkAccounts(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/markets")
    public ResponseEntity<List<VkMarketDTO>> getAllMarkets(
            @RequestParam(name = "tg_id") Long tgUserId
    ) {
        try {
            return ResponseEntity.ok(userService.getAllUserMarkets(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/vk-logout")
    public ResponseEntity<String> logout(
            @RequestParam(name = "vk_account_id") UUID vkAccountId
    ) {
        try {
            userService.logoutVkAccount(vkAccountId);
            return ResponseEntity.ok("Logout successful!");
        } catch (Exception e) {
            return  ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(
            @RequestParam(name = "tg_id") Long tgUserId) {
        try {
            if (!userService.existsUserByTgId(tgUserId)) {
                User user = userService.createUser(tgUserId);
                userService.saveUser(user);
            }
            return ResponseEntity.ok("Successfully created user!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error to create user");
        }
    }

    @GetMapping("/check-active-account")
    public ResponseEntity<Boolean> checkUserActiveAccount(
            @RequestParam(name = "tg_id") Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.checkActiveVkAccount(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/check-active-market")
    public ResponseEntity<Boolean> checkUserActiveMarket(
            @RequestParam(name = "tg_id") Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.checkUserActiveMarket(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // @GetMapping("/check-active-market")
    // public ResponseEntity<Boolean> checkUserActiveMarket(
    //         @RequestParam(name = "tg_id") Long tgUserId) {
    //     try {
    //         return ResponseEntity.ok(userService.checkUserActiveMarket(tgUserId));
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(null);
    //     }
    // }

    @PatchMapping("/update-active-market")
    public ResponseEntity<String> updateActiveMarket(
            @RequestParam(name = "tg_id") Long tgUserId,
            @RequestParam(name = "market_id") UUID marketId) {
        try {
            userService.updateActiveMarket(tgUserId, marketId);
            return ResponseEntity.ok("Successfully updated active market!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error to update active market");
        }
    }

}
