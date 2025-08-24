package com.telegramapi.vkparser.services;

import java.util.List;

public interface RedisService {
    <T> T getValue(String key, Class<T> clazz);

    <T> List<T> getListValue(String key, Class<T> elementClass);

    void setValue(String key, Object value);

    void deleteKey(String key);

    boolean hasKey(String key);
}
