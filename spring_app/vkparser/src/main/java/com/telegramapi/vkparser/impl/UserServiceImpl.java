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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMarketServiceImpl userMarketService;
    private final BlockingServiceImpl blockingService;
    private final VkAccountServiceImpl vkAccountService;
    private final VkMarketServiceImpl vkMarketService;
    private final VkServiceImpl vkService;

    public UserServiceImpl(UserMarketServiceImpl userMarketService,
                           BlockingServiceImpl blockingService,
                           UserRepository userRepository,
                           VkAccountServiceImpl vkAccountService,
                           VkMarketServiceImpl vkMarketService,
                           VkServiceImpl vkService) {
        this.userMarketService = userMarketService;
        this.blockingService = blockingService;
        this.userRepository = userRepository;
        this.vkAccountService = vkAccountService;
        this.vkMarketService = vkMarketService;
        this.vkService = vkService;
    }

    @Override
    public User getUserByTgId(Long tgUserId) {
        log.debug("Fetching user by Telegram ID: {}", tgUserId);
        return userRepository.findByTgUserId(tgUserId);
    }

    @Override
    public Mono<Void> syncUserMarkets(VkAccount vkAccount, List<VkMarket> vkMarkets) {
        log.info("Starting sync of {} VK markets for VK account ID: {}", vkMarkets.size(), vkAccount.getId());

        return Flux.fromIterable(vkMarkets)
                .flatMap(vkMarket ->
                        blockingService.runBlocking(() -> vkMarketService.saveVkMarket(vkMarket))
                                .thenReturn(vkMarket)
                                .flatMap(savedVkMarket -> {
                                    UserMarket userMarket = userMarketService.createUserMarket(vkAccount, savedVkMarket);
                                    return blockingService.runBlocking(() -> userMarketService.saveUserMarket(userMarket));
                                })
                )
                .doOnSubscribe(sub -> log.info("User market synchronization initiated for VK account ID: {}", vkAccount.getId()))
                .doOnError(e -> log.error("Error during user market synchronization for VK account ID: {}", vkAccount.getId(), e))
                .then()
                .doOnSuccess(unused -> log.info("User market synchronization completed for VK account ID: {}", vkAccount.getId()))
                .doOnTerminate(() -> log.info("User markets synchronization finished."))
                .then();
    }

    @Override
    public Boolean existsUserByTgId(Long tgUserId) {
        boolean exists = userRepository.existsByTgUserId(tgUserId);
        log.debug("Checking existence of user by Telegram ID {}: {}", tgUserId, exists);
        return exists;
    }

    public List<VkAccountDTO> getAllUserVkAccounts(Long tgUserId) {
        log.info("Fetching all VK accounts for user ID: {}", tgUserId);
        List<VkAccount> vkAccounts = vkAccountService.getAllUserVkAccounts(tgUserId);
        return vkAccounts
                .stream()
                .map(vkAccount -> new VkAccountDTO(
                        vkAccount.getId(),
                        vkAccount.getFirstName(),
                        vkAccount.getLastName(),
                        vkAccount.getScreenName(),
                        vkAccount.getActive()
                ))
                .toList();
    }

    public List<VkMarketDTO> getAllUserMarkets(Long tgUserId) {
        log.info("Fetching VK markets for user ID: {}", tgUserId);
        VkAccount activeVkAccount = vkAccountService.getActiveAccount(tgUserId);
        if (activeVkAccount != null) {
            List<UserMarket> userMarkets = userMarketService.getAllUserMarkets(activeVkAccount);
            return userMarkets
                    .stream()
                    .map(userMarket -> new VkMarketDTO(
                            userMarket.getId(),
                            userMarket.getVkMarket().getMarketName(),
                            userMarket.getVkMarket().getMembersCount(),
                            userMarket.getActive()
                    ))
                    .toList();
        } else {
            log.warn("No active VK account found for user ID: {}", tgUserId);
            throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
        }
    }

    @Override
    public User createUser(Long tgUserId) {
        log.info("Creating new user with Telegram ID: {}", tgUserId);
        User user = new User();
        user.setTgUserId(tgUserId);
        return user;
    }

    @Override
    public void saveUser(User user) {
        log.info("Saving user with Telegram ID: {}", user.getTgUserId());
        userRepository.save(user);
    }

    @Override
    public FullUserInfoDTO getFullUserInfo(Long tgUserId) {
        log.info("Fetching full user info for Telegram ID: {}", tgUserId);
//        User user = getUserByTgId(tgUserId);
        List<VkAccountDTO> vkAccounts = getAllUserVkAccounts(tgUserId);
        List<VkMarketDTO> userMarkets = getAllUserMarkets(tgUserId);
        return new FullUserInfoDTO(vkAccounts, userMarkets);
    }

    public Boolean checkActiveVkAccount(Long tgUserId) {
        log.debug("Checking active account for Telegram user ID: {}", tgUserId);
        return vkAccountService.getActiveAccount(tgUserId) != null;
    }

    public Boolean checkUserActiveMarket(Long tgUserId) {
        log.debug("Checking active market for Telegram user ID: {}", tgUserId);
//        User user = getUserByTgId(tgUserId);
        VkAccount activeAccount = vkAccountService.getActiveAccount(tgUserId);
        if (activeAccount == null) {
            log.warn("No active VK market found for user ID: {}", tgUserId);
            return false;
        }
        UUID vkAccountId = activeAccount.getId();
        boolean hasMarket = userMarketService.getActiveUserMarket(vkAccountId) != null;
        log.debug("User has active market: {}", hasMarket);
        return hasMarket;
    }

    public void updateActiveMarket(UUID vkAccountId, UUID userMarketId) {
        log.info("Updating active market for VK account ID: {} to market ID: {}", vkAccountId, userMarketId);
        userMarketService.setActiveUserMarket(vkAccountId, userMarketId);
    }

    //todo
    public void logoutVkAccount(UUID vkAccountId) {
        VkAccount vkAccount = vkAccountService.getVkAccountById(vkAccountId);
        String accessToken = vkAccount.getAccessToken();
        vkService.logout(accessToken);
    }
}
