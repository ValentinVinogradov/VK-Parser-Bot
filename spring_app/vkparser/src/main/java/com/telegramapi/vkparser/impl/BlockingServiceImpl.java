package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.services.BlockingService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Service
public class BlockingServiceImpl implements BlockingService {

    public <T> Mono<T> fromBlocking(Supplier<T> supplier) {
        Callable<T> callable = supplier::get;
        return Mono.fromCallable(callable).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> runBlocking(Runnable runnable) {
        return Mono.fromRunnable(runnable)
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
