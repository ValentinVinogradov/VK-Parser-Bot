package com.telegramapi.vkparser.models;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_markets")
public class UserMarkets {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long tgUserId;

    private Long vkUserId;

    private Long groupId;

    private String groupName;


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

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
