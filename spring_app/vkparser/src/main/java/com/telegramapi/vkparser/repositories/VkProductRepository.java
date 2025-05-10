package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.models.VkProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VkProductRepository extends JpaRepository<VkProduct, UUID> {

    @Query("SELECT vp.id FROM VkProduct vp WHERE vp.vkMarket = :vkMarket")
    Page<UUID> findIdsByVkMarket(VkMarket vkMarket, Pageable pageable);

    List<VkProduct> findAllByVkMarket(VkMarket vkMarket);
}