package com.telegramapi.vkparser.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FullUserInfoDTO (
        @JsonProperty("accounts") List<VkAccountDTO> vkAccounts,
        @JsonProperty("markets") List<VkMarketDTO> userMarkets
) {}
