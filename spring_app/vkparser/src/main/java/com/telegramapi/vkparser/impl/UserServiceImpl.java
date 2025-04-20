package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.repositories.UserRepository;
import com.telegramapi.vkparser.services.UserService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserServiceImpl implements UserService {

    private final UserMarketServiceImpl userMarketService;
    private final BlockingServiceImpl blockingService;
    private final UserRepository userRepository;
    private final VkServiceImpl vkService;


    public UserServiceImpl(UserMarketServiceImpl userMarketService, BlockingServiceImpl blockingService,
                           UserRepository userRepository, VkServiceImpl vkService) {
        this.userMarketService = userMarketService;
        this.blockingService = blockingService;
        this.userRepository = userRepository;
        this.vkService = vkService;
    }

    @Override
    public User getUserByTgId(Long tgUserId) {
        return userRepository.findByTgUserId(tgUserId);
    }


    @Override
    public Mono<Void> syncUserMarkets(Long tgUserId, Long vkUserId, String accessToken) {
        System.out.println("1: " + accessToken);
        return vkService.getUserMarkets(tgUserId, vkUserId, accessToken)
                .flatMap(userMarkets ->
                        blockingService.runBlocking(() ->
                                userMarketService.saveAllUserMarkets(userMarkets)));
    }


    @Override
    public Boolean existsUserByTgId(Long tgUserId) {
        return userRepository.existsByTgUserId(tgUserId);
    }

    @Override
    public User createUser(Long tgUserId) {
        User user = new User();
        user.setTgUserId(tgUserId);
        return user;
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }



}
