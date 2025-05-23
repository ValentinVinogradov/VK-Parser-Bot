package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.VkAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VkAccountRepository extends JpaRepository<VkAccount, UUID> {

    @Modifying
    @Query("UPDATE VkAccount va SET va.isActive = false WHERE va.user = :user")
    void deactivateVkAccount(User user);

    @Modifying
    @Query("UPDATE VkAccount va SET va.isActive = true WHERE va.id = :vkAccountId")
    void activateVkAccount(UUID vkAccountId);

    Optional<VkAccount> findByUserAndIsActiveTrue(User user);

    List<VkAccount> findAllByUser(User user);

    boolean existsByUser(User user);
}
