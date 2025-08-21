package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkMarketCacheDTO;
import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.dto.VkProductResponseDTO;
import com.telegramapi.vkparser.models.*;
import com.telegramapi.vkparser.repositories.VkProductRepository;
import com.telegramapi.vkparser.services.VkMarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;


@Service
public class VkProductServiceImpl {
    private final String STATE = System.getenv("VK_STATE");
    private static final Logger log = LoggerFactory.getLogger(VkProductServiceImpl.class);
    private final VkProductRepository vkProductRepository;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final UserServiceImpl userService;
    private final VkMarketService vkMarketService;
    private final VkAccountServiceImpl vkAccountService;
    private final UserMarketServiceImpl userMarketService;
    private final TokenServiceImpl tokenService;
    private final RedisServiceImpl redisService;


    public VkProductServiceImpl(VkProductRepository vkProductRepository,
                                VkServiceImpl vkService,
                                BlockingServiceImpl blockingService,
                                UserServiceImpl userService, VkMarketService vkMarketService,
                                VkAccountServiceImpl vkAccountService,
                                UserMarketServiceImpl userMarketService, TokenServiceImpl tokenService, RedisServiceImpl redisService) {
        this.vkProductRepository = vkProductRepository;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.userService = userService;
        this.vkMarketService = vkMarketService;
        this.vkAccountService = vkAccountService;
        this.userMarketService = userMarketService;
        this.tokenService = tokenService;
        this.redisService = redisService;
    }

    public VkProductResponseDTO getVkProductsFromDatabase(VkMarket vkMarket, int count, int page) {
        log.debug("Fetching VK products from database for market ID: {}, page: {}, count: {}", vkMarket.getId(), page, count);
        PageRequest pageRequest = PageRequest.of(page - 1, count, Sort.by("title").ascending());
        Page<VkProduct> vkProductsPage = vkProductRepository.findProductsByVkMarket(vkMarket, pageRequest);
        List<VkProduct> vkProductsContent = vkProductsPage.getContent();

        return convertVkProductsToDto(vkProductsContent, page, vkProductsPage.getTotalElements());
    }


    public List<VkProduct> getAllVkProductsFromDatabase(VkMarket vkMarket) {
        log.debug("Fetching all VK products from database for market ID: {}", vkMarket.getId());
        return vkProductRepository.findAllByVkMarket(vkMarket);
    }

    public VkProductResponseDTO convertVkProductsToDto(List<VkProduct> vkProducts, Integer page, Long count) {
        log.debug("Converting VK products to ID DTO. Count: {}", count);
        return new VkProductResponseDTO(
                vkProducts.stream().map(this::convertVkProductToFullDto).toList(),
                page,
                count);
    }

    public VkProductDTO convertVkProductToFullDto(VkProduct vkProduct) {
        return new VkProductDTO(
                vkProduct.getId(),
                vkProduct.getTitle(),
                vkProduct.getCategory(),
                vkProduct.getDescription(),
                vkProduct.getPrice(),
                vkProduct.getAvailability(),
                vkProduct.getPhotoUrls(),
                vkProduct.getStockQuantity(),
                vkProduct.getLikesCount(),
                vkProduct.getRepostCount(),
                vkProduct.getViewsCount(),
                vkProduct.getReviewsCount(),
                vkProduct.getCreatedAt()
        );
    }

    public void syncProducts(VkMarket vkMarket, VkAccountCacheDTO vkDTO) {
        Long vkMarketId = vkMarket.getMarketVkId();
        log.info("Starting synchronization for VK market ID: {}", vkMarketId);

        tokenService.getFreshAccessToken(vkDTO, STATE)
                .flatMap(accessToken -> {
                    log.info("Fetching VK products via VK API for market ID: {}", vkMarketId);
                    return vkService.getProducts(accessToken, vkMarketId)
                            .flatMapMany(Flux::fromIterable)
                            .flatMap(vkProduct -> blockingService.runBlocking(() -> {
                                vkProduct.setVkMarket(vkMarket);
                                saveVkProduct(vkProduct);
                            }))
                            .then(); // заменяем collectList на then(), т.к. результат нам не нужен
                })
                .doOnSuccess(v -> log.info("Successfully synchronized VK products for market ID: {}", vkMarketId))
                .doOnError(ex -> log.error("Failed to synchronize VK products", ex))
                .block(); // блокируем, чтобы метод отработал синхронно и вернул управление только после завершения
    }


    public VkProductResponseDTO getVkProducts(Long tgUserId, int count, int page) {
        log.info("Request to get VK products for Telegram user ID: {}", tgUserId);
        try {
            VkProductResponseDTO vkCachedProducts = redisService
                    .getValue(String.format("products:%s:%d", tgUserId, page), VkProductResponseDTO.class);
            log.info("Cached vk products: {}", vkCachedProducts);
            if (vkCachedProducts != null) {
                return vkCachedProducts;
            }
            VkAccountCacheDTO cacheVkAccount = redisService
                    .getValue(String.format("user:%s:active_vk_account", tgUserId), VkAccountCacheDTO.class);
            log.info("Cached vk account: {}", cacheVkAccount);
            if (cacheVkAccount == null) {
                log.info("No found cached vk account");
                VkAccount activeVkAccount = vkAccountService.getActiveAccount(tgUserId);
                if (activeVkAccount == null) {
                    log.warn("No active VK account found for user ID: {}", tgUserId);
                    throw new RuntimeException("No active VK account found for user ID: " + tgUserId);
                }
                cacheVkAccount = new VkAccountCacheDTO(
                        activeVkAccount.getId(),
                        activeVkAccount.getAccessToken(),
                        activeVkAccount.getRefreshToken(),
                        activeVkAccount.getDeviceId(),
                        activeVkAccount.getExpiresAt());
                log.info("Vk account id from db: {}", activeVkAccount.getId());
                redisService.setValue(String.format("user:%s:active_vk_account", tgUserId), cacheVkAccount);
            }

            VkMarket activeVkMarket;
            VkMarketCacheDTO cachedMarket = redisService
                    .getValue(String.format("user:%s:active_vk_market", tgUserId), VkMarketCacheDTO.class);
            if (cachedMarket == null) {
                UserMarket userMarket = userMarketService.getActiveUserMarket(cacheVkAccount.id());
                if (userMarket == null) {
                    throw new IllegalStateException("No active market found for VK account");
                }
                activeVkMarket = userMarket.getVkMarket();
                VkMarketCacheDTO cacheMarketDTO = new VkMarketCacheDTO(userMarket.getId(),
                        activeVkMarket.getMarketVkId());
                redisService.setValue(String.format("user:%s:active_vk_market", tgUserId),
                        cacheMarketDTO);
            } else {
                activeVkMarket = vkMarketService.getMarketById(cachedMarket.marketVkId());
            }


            VkProductResponseDTO productsFromDb = getVkProductsFromDatabase(activeVkMarket, count, page);


            if (productsFromDb.products().isEmpty()) {
                log.info("No stored products found. Synchronizing from VK...");
                syncProducts(activeVkMarket, cacheVkAccount);
                productsFromDb = getVkProductsFromDatabase(activeVkMarket, count, page);
            }

            if (productsFromDb.products().isEmpty()) {
                log.warn("Synchronization returned empty product list");
                return new VkProductResponseDTO(List.of(), 1, 0L);
            }
            redisService.setValue(String.format("products:%s:%d", tgUserId, page), productsFromDb);
            log.info("Returning {} products from database", productsFromDb.products().size());
            return productsFromDb;
        } catch (Exception e) {
            log.error("Failed to fetch VK products for tgUserId={}", tgUserId, e);
            throw new RuntimeException("Unable to retrieve VK products", e);
        }
    }

    public void saveVkProduct(VkProduct vkProduct) {
        log.debug("Saving VK product with ID: {}", vkProduct.getVkProductId());
        vkProductRepository.save(vkProduct);
    }

    public VkProductDTO getVkProductById(UUID vkProductId) {
        log.debug("Fetching VK product with ID: {}", vkProductId);
        return vkProductRepository.findById(vkProductId)
                .map(this::convertVkProductToFullDto)
                .orElseThrow(() -> {
                    log.warn("VK product with ID {} not found", vkProductId);
                    return new NoSuchElementException("VK product not found");
                });
    }
}
