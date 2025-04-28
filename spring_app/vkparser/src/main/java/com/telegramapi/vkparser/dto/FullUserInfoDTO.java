package com.telegramapi.vkparser.dto;

import java.util.List;

public class FullUserInfoDTO {
    private List<VkAccountDTO> vkAccounts;

    private List<VkMarketDTO> userMarkets;

    public List<VkMarketDTO> getUserMarkets() {
        return userMarkets;
    }

    public void setUserMarkets(List<VkMarketDTO> userMarkets) {
        this.userMarkets = userMarkets;
    }

    public List<VkAccountDTO> getVkAccounts() {
        return vkAccounts;
    }

    public void setVkAccounts(List<VkAccountDTO> vkAccounts) {
        this.vkAccounts = vkAccounts;
    }

}
