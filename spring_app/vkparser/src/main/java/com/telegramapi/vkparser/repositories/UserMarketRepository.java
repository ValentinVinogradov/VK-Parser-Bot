package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.UserMarket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMarketRepository extends JpaRepository<UserMarket, Long> {
    // List<UserMarket> findAllByVkAccount(VkAccount vkAccount);
    List<UserMarket> findAllByVkAccount_Id(UUID id);

    @Modifying
    @Query("UPDATE UserMarket um SET um.isActive = false WHERE um.vkAccount.id = :vkAccountId AND um.isActive = true")
    void deactivateUserMarket(@Param("vkAccountId") UUID vkAccountId);

    @Modifying
    @Query("UPDATE UserMarket um SET um.isActive = true WHERE um.id = :userMarketId")
    void activateUserMarket(@Param("userMarketId") UUID userMarketId);

    Optional<UserMarket> findByVkAccount_IdAndIsActiveTrue(UUID id);
}
