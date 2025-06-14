package com.telegramapi.vkparser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VkTokenResponseDTO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("user_id") long userId,
        @JsonProperty("state") String state
) {}

