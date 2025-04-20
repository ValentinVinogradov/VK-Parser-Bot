package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Boolean existsByTgUserId(Long tgUserId);
    User findByTgUserId(Long tgUserId);
}
