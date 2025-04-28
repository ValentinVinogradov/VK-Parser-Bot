package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMarketRepository extends JpaRepository<UserMarket, Long> {
    Optional<UserMarket> findByVkAccountAndIsActiveTrue(VkAccount vkAccount);
    List<UserMarket> findAllByVkAccount(VkAccount vkAccount);
}
