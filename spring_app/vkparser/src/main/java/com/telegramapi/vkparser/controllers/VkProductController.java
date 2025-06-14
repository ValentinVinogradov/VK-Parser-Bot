package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.dto.VkProductResponseDTO;
import com.telegramapi.vkparser.impl.VkProductServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
public class VkProductController {
    private final VkProductServiceImpl vkProductService;


    public VkProductController(VkProductServiceImpl vkProductService) {
        this.vkProductService = vkProductService;
    }

    @GetMapping("/get-page")
    public ResponseEntity<VkProductResponseDTO> getProducts(
            @RequestParam(name = "tg_id") Long tgUserId,
            @RequestParam(name = "count") int count,
            @RequestParam(name = "page") int page) {
        try {
            return ResponseEntity.ok(vkProductService.getVkProducts(tgUserId, count, page));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<VkProductDTO> getProduct(
            @PathVariable(name = "id") UUID vkProductId) {
        try {
            return ResponseEntity.ok(vkProductService.getVkProductById(vkProductId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}

