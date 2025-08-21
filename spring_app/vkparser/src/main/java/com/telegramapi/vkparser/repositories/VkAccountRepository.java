package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VkAccountRepository extends JpaRepository<VkAccount, UUID> {

    @Modifying
    @Query("UPDATE VkAccount va SET va.isActive = false WHERE va.user.tgUserId = :tgUserId AND va.isActive = true")
    void deactivateVkAccount(@Param("tgUserId") Long tgUserId);


    @Modifying
    @Query("UPDATE VkAccount va SET va.isActive = true WHERE va.id = :vkAccountId")
    void activateVkAccount(UUID vkAccountId);

    Optional<VkAccount> findByUser_TgUserIdAndIsActiveTrue(Long tgUserId);

    List<VkAccount> findAllByUser_TgUserId(Long tgUserId);

    boolean existsByUser(User user);

    boolean existsByVkUserId(Long vkUserId);

    boolean existsByUser_TgUserId(Long tgUserId);

    Optional<VkAccount> findByVkUserId(Long vkUserId);

    @Modifying
    @Query("UPDATE VkAccount v SET " +
            "v.accessToken = :accessToken, " +
            "v.refreshToken = :refreshToken, " +
            "v.expiresAt = :expiresAt WHERE v.id = :id")
    void updateVkAccountFields(UUID id, String accessToken, String refreshToken, LocalDateTime expiresAt);
}
