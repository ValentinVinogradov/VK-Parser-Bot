package com.telegramapi.vkparser.services;

import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public interface BlockingService {
    <T> Mono<T> fromBlocking(Supplier<T> supplier);
    Mono<Void> runBlocking(Runnable runnable);
}
