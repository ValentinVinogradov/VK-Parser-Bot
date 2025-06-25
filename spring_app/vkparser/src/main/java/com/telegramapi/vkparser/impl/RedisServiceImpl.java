package com.telegramapi.vkparser.impl;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisServiceImpl {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public RedisServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper
            ) {
        this.redisTemplate = redisTemplate;
        this.redisObjectMapper = redisObjectMapper;
    }

    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return redisObjectMapper.convertValue(value, clazz);
    }

    public <T> List<T> getListValue(String key, Class<T> elementClass) {
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) return List.of();

        return redisObjectMapper.convertValue(
            raw,
            redisObjectMapper.getTypeFactory().constructCollectionType(List.class, elementClass)
        );
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setValueWithTTL(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }
}

