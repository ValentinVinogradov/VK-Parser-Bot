package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.impl.UserServiceImpl;
import com.telegramapi.vkparser.models.VkMarket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @GetMapping("/groups/all")
    public ResponseEntity<List<VkMarket>> getUserMarkets(@RequestParam Long tgUserId) {
        try {
            return ResponseEntity.ok(userService.getUserMarkets(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }



}
