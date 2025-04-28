package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.impl.VkProductServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/products")
public class VkProductController {
    private final VkProductServiceImpl vkProductService;


    public VkProductController(VkProductServiceImpl vkProductService) {
        this.vkProductService = vkProductService;
    }

    //todo
    @GetMapping("/all")
    public ResponseEntity<List<VkProductDTO>> getAllProducts(@RequestParam(name = "tg_id") Long tgUserId) {
        try {
            System.out.println("Контроллер товаров");
            return ResponseEntity.ok(vkProductService.getAllVkProducts(tgUserId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }
}

