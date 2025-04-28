package com.telegramapi.vkparser.models;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "vk_markets")
public class VkMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_name")
    private String marketName;

    @Column(name = "market_vk_id")
    private Long marketVkId;

    @Column(name = "market_url")
    private String marketUrl;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public Long getMarketVkId() {
        return marketVkId;
    }

    public void setMarketVkId(Long marketVkId) {
        this.marketVkId = marketVkId;
    }

    public String getMarketUrl() {
        return marketUrl;
    }

    public void setMarketUrl(String marketUrl) {
        this.marketUrl = marketUrl;
    }
}
