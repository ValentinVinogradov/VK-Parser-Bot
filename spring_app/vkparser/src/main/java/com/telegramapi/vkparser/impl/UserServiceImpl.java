package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.UserRepository;
import com.telegramapi.vkparser.services.UserService;
import com.telegramapi.vkparser.services.VkMarketService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMarketServiceImpl userMarketService;
    private final BlockingServiceImpl blockingService;
    private final VkServiceImpl vkService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkMarketServiceImpl vkMarketService;



    public UserServiceImpl(UserMarketServiceImpl userMarketService,
                           BlockingServiceImpl blockingService,
                           UserRepository userRepository,
                           VkServiceImpl vkService,
                           VkAccountServiceImpl vkAccountService, VkMarketServiceImpl vkMarketService) {
        this.userMarketService = userMarketService;
        this.blockingService = blockingService;
        this.userRepository = userRepository;
        this.vkService = vkService;
        this.vkAccountService = vkAccountService;
        this.vkMarketService = vkMarketService;
    }

    @Override
    public User getUserByTgId(Long tgUserId) {
        return userRepository.findByTgUserId(tgUserId);
    }


    @Override
    public Mono<Void> syncUserMarkets(Long tgUserId, Long vkUserId, String accessToken) {
        return vkService.getUserMarkets(tgUserId, vkUserId, accessToken)
                .flatMap(vkMarkets ->
                        Flux.fromIterable(vkMarkets)
                                .flatMap(vkMarket -> {
                                    vkMarketService.saveVkMarket(vkMarket);
                                    UserMarket userMarket = new UserMarket();
                                    userMarket.setTgUserId(tgUserId);
                                    userMarket.setVkUserId(vkUserId);
                                    userMarket.setVkMarket(vkMarket);
                                    return blockingService.runBlocking(() ->
                                            userMarketService.saveUserMarket(userMarket));
                                })
                                .then()
                .doOnTerminate(() -> System.out.println("User markets synchronization finished."))
                .doOnSubscribe(s -> System.out.println("User market synchronization started..."))
                .doOnSuccess(s -> System.out.println("User market synchronization completed successfully!")));
    }

    public List<VkMarket> getUserMarkets(Long tgUserId) {
        User user = getUserByTgId(tgUserId);
        VkAccount activeVkAccount = vkAccountService.getActiveAccount(user);
        Long vkUserId = activeVkAccount.getVkUserId();
        List<UserMarket> userMarkets = userMarketService.getAllUserMarkets(vkUserId);
        return userMarkets
                .stream()
                .map(UserMarket::getVkMarket)
                .toList();
    }

    //todo сделать как то рефреш аксесс токена


    //todo организовать метод для установки активного паблика

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
    public void saveUser(User user) {
        userRepository.save(user);
    }



}
