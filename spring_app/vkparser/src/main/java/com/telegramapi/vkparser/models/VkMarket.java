package com.telegramapi.vkparser.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vk_markets")
public class VkMarket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_name", nullable = false)
    private String marketName;

    @Column(name = "market_vk_id", nullable = false, unique = true)
    private Long marketVkId;

    @Column(name = "members_count", nullable = false)
    private int membersCount;

    @Column(name = "market_url", nullable = false)
    private String marketUrl;

    @OneToMany(mappedBy = "vkMarket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserMarket> userMarkets = new ArrayList<>();

    @OneToMany(mappedBy = "vkMarket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VkProduct> vkProducts = new ArrayList<>();


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

    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }
}
