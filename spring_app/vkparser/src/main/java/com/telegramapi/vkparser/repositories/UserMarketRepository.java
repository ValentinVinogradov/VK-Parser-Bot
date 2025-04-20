package com.telegramapi.vkparser.repositories;

import com.telegramapi.vkparser.models.UserMarket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMarketRepository extends JpaRepository<UserMarket, Long> {
}
