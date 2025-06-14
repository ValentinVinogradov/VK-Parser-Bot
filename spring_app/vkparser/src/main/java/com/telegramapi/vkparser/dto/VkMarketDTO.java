package com.telegramapi.vkparser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VkMarketDTO (
    UUID id,
    String name,
    @JsonProperty("members_count") int membersCount,
    @JsonProperty("is_active") boolean isActive
) {}
