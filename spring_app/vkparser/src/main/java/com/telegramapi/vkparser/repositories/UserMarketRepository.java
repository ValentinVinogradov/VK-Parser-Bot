package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.UserMarket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMarketRepository extends JpaRepository<UserMarket, Long> {
    List<UserMarket> findAllByVkUserId(Long vkUserId);
}
