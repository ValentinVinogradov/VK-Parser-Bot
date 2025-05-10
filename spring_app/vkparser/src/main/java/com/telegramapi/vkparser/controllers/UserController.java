package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.FullUserInfoDTO;
import com.telegramapi.vkparser.impl.UserServiceImpl;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkMarket;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<FullUserInfoDTO> getFullUserInfo(@RequestParam(name = "tg_id") Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.getFullUserInfo(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestParam(name = "tg_id") Long tgUserId) {
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

    @GetMapping("/check-login")
    public ResponseEntity<Boolean> checkUserLogin(@RequestParam(name = "tg_id") Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.checkUserLogin(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/check-active-market")
    public ResponseEntity<Boolean> checkUserActiveMarket(@RequestParam(name = "tg_id") Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.checkUserActiveMarket(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    //todo нахуя он подумать
//    @GetMapping("/groups/all")
//    public ResponseEntity<List<VkMarket>> getUserMarkets(@RequestParam Long tgUserId) {
//        try {
//            return ResponseEntity.ok(userService.getUserMarkets(tgUserId));
//        } catch (Exception e) {
//            return ResponseEntity.notFound().build();
//        }
//    }

    @PatchMapping("/update-active-market")
    public ResponseEntity<String> updateActiveMarket(
            @RequestParam(name = "account_id") UUID vkAccountId,
            @RequestParam(name = "market_id") UUID marketId) {
        try {
            System.out.println("1");
            userService.updateActiveMarket(vkAccountId, marketId);
            System.out.println("2");
            return ResponseEntity.ok("Successfully updated active market!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error to update active market");
        }
    }

}
