package com.telegramapi.vkparser.enums;

public enum ResponseStatusEnum {
    SUCCESS("success"),
    ERROR("error");

    private final String value;

    ResponseStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }


}
