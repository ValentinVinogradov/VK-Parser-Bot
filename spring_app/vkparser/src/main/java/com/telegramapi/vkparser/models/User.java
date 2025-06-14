package com.telegramapi.vkparser.models;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;

    @Id
    @Column(name = "tg_user_id", unique = true, nullable = false)
    private Long tgUserId;


    public Long getTgUserId() {
        return tgUserId;
    }

    public void setTgUserId(Long tgUserId) {
        this.tgUserId = tgUserId;
    }
}
