package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.FullUserInfoDTO;
import com.telegramapi.vkparser.dto.VkAccountDTO;
import com.telegramapi.vkparser.dto.VkMarketDTO;
import com.telegramapi.vkparser.models.User;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkAccount;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.repositories.UserRepository;
import com.telegramapi.vkparser.services.UserService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMarketServiceImpl userMarketService;
    private final BlockingServiceImpl blockingService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkMarketServiceImpl vkMarketService;



    public UserServiceImpl(UserMarketServiceImpl userMarketService,
                           BlockingServiceImpl blockingService,
                           UserRepository userRepository,
                           VkAccountServiceImpl vkAccountService,
                           VkMarketServiceImpl vkMarketService) {
        this.userMarketService = userMarketService;
        this.blockingService = blockingService;
        this.userRepository = userRepository;
        this.vkAccountService = vkAccountService;
        this.vkMarketService = vkMarketService;
    }

    @Override
    public User getUserByTgId(Long tgUserId) {
        return userRepository.findByTgUserId(tgUserId);
    }


    @Override
    public Mono<Void> syncUserMarkets(VkAccount vkAccount, List<VkMarket> vkMarkets) {
        return Flux.fromIterable(vkMarkets)
                .flatMap(vkMarket ->
                    blockingService.runBlocking(() -> vkMarketService.saveVkMarket(vkMarket))
                        .thenReturn(vkMarket)
                        .flatMap(savedVkMarket -> {
                            UserMarket userMarket = userMarketService.createUserMarket(vkAccount, savedVkMarket);
                            return blockingService.runBlocking(() -> userMarketService.saveUserMarket(userMarket));
                        })
                )
                .then()
                .doOnTerminate(() -> System.out.println("User markets synchronization finished."))
                .doOnSubscribe(s -> System.out.println("User market synchronization started..."))
                .doOnSuccess(s -> System.out.println("User market synchronization completed successfully!"));
    }

//    public List<VkMarket> getUserMarkets(Long tgUserId) {
//        User user = getUserByTgId(tgUserId);
//        VkAccount activeVkAccount = vkAccountService.getActiveAccount(user);
//        List<UserMarket> userMarkets = userMarketService.getAllUserMarkets(activeVkAccount);
//        return userMarkets
//                .stream()
//                .map(UserMarket::getVkMarket)
//                .toList();
//    }

    //todo сделать как то рефреш аксесс токена


    //todo организовать метод для установки активного паблика

    @Override
    public Boolean existsUserByTgId(Long tgUserId) {
        return userRepository.existsByTgUserId(tgUserId);
    }

    public List<VkAccountDTO> getAllUserVkAccounts(User user) {
        List<VkAccount> vkAccounts = vkAccountService.getAllUserVkAccounts(user);
        return vkAccounts
                .stream()
                .map(vkAccount -> {
                    VkAccountDTO vkAccountDTO = new VkAccountDTO();
                    vkAccountDTO.setId(vkAccount.getId());
                    vkAccountDTO.setActive(vkAccount.getActive());
                    vkAccountDTO.setFirstName(vkAccount.getFirstName());
                    vkAccountDTO.setLastName(vkAccount.getLastName());
                    vkAccountDTO.setScreenName(vkAccount.getScreenName());
                    return vkAccountDTO;
                })
                .toList();
    }

    public List<VkMarketDTO> getAllUserMarkets(User user) {
        VkAccount activeVkAccount = vkAccountService.getActiveAccount(user);
        if (activeVkAccount != null) {
            List<UserMarket> userMarkets = userMarketService.getAllUserMarkets(activeVkAccount);
            return userMarkets
                    .stream()
                    .map(userMarket -> {
                        VkMarketDTO vkMarketDTO = new VkMarketDTO();
                        vkMarketDTO.setId(userMarket.getId());
                        vkMarketDTO.setName(userMarket.getVkMarket().getMarketName());
                        vkMarketDTO.setActive(userMarket.getActive());
                        return vkMarketDTO;
                    })
                    .toList();
        } else {
            return List.of();
        }
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

    @Override
    public FullUserInfoDTO getFullUserInfo(Long tgUserId) {
        User user = getUserByTgId(tgUserId);
        List<VkAccountDTO> vkAccounts = getAllUserVkAccounts(user);
        List<VkMarketDTO> userMarkets = getAllUserMarkets(user);
        FullUserInfoDTO fullUserInfoDTO = new FullUserInfoDTO();
        fullUserInfoDTO.setUserMarkets(userMarkets);
        fullUserInfoDTO.setVkAccounts(vkAccounts);
        return fullUserInfoDTO;
    }


    public Boolean checkUserLogin(Long tgUserId) {
        User user = getUserByTgId(tgUserId);
        return vkAccountService.existsVkAccountByUser(user);
    }

    public Boolean checkUserActiveMarket(Long tgUserId) {
        User user = getUserByTgId(tgUserId);
        UUID vkAccountId = vkAccountService.getActiveAccount(user).getId();
        return userMarketService.getActiveUserMarket(vkAccountId) != null;
    }

    public void updateActiveMarket(UUID marketId, UUID vkAccountId) {
        userMarketService.setActiveUserMarket(marketId, vkAccountId);
    }
}
