package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.VkAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VkAccountRepository extends JpaRepository<VkAccount, UUID> {
}
