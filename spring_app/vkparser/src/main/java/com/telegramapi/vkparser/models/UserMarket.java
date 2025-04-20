package com.telegramapi.vkparser.models;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_markets")
public class UserMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tg_user_id")
    private Long tgUserId;

    @Column(name = "vk_user_id")
    private Long vkUserId;

    @ManyToOne
    private VkMarket vkMarket;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getTgUserId() {
        return tgUserId;
    }

    public void setTgUserId(Long tgUserId) {
        this.tgUserId = tgUserId;
    }

    public Long getVkUserId() {
        return vkUserId;
    }

    public void setVkUserId(Long vkUserId) {
        this.vkUserId = vkUserId;
    }


    public VkMarket getVkMarket() {
        return vkMarket;
    }

    public void setVkMarket(VkMarket vkMarket) {
        this.vkMarket = vkMarket;
    }
}
