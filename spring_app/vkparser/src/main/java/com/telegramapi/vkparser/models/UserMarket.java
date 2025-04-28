package com.telegramapi.vkparser.models;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_markets")
public class UserMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "vk_account_id", nullable = false)
    private VkAccount vkAccount;

//    @Column(name = "tg_user_id")
//    private Long tgUserId;
//
//    @Column(name = "vk_user_id")
//    private Long vkUserId;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @ManyToOne
    private VkMarket vkMarket;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VkMarket getVkMarket() {
        return vkMarket;
    }

    public void setVkMarket(VkMarket vkMarket) {
        this.vkMarket = vkMarket;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public VkAccount getVkAccount() {
        return vkAccount;
    }

    public void setVkAccount(VkAccount vkAccount) {
        this.vkAccount = vkAccount;
    }
}
