package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.models.VkProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VkProductRepository extends JpaRepository<VkProduct, UUID> {
    List<VkProduct> findAllByVkMarket(VkMarket vkMarket);
}