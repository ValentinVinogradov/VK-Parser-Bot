package com.telegramapi.vkparser.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VkUserInfoDTO (
    @JsonProperty("first_name")
    String firstName,
    @JsonProperty("last_name")
    String lastName,
    @JsonProperty("screen_name")
    String screenName
) {}
