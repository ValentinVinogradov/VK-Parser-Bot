package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.VkMarket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface VkMarketRepository extends JpaRepository<VkMarket, UUID> {
    Boolean existsByMarketVkId(Long vkMarketId);

    VkMarket getByMarketVkId(Long marketVkId);
}
