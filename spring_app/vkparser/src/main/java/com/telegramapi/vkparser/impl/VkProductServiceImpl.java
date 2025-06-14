package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.dto.VkProductResponseDTO;
import com.telegramapi.vkparser.models.*;
import com.telegramapi.vkparser.repositories.VkProductRepository;
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
    private final VkAccountServiceImpl vkAccountService;
    private final UserMarketServiceImpl userMarketService;
    private final TokenServiceImpl tokenService;


    public VkProductServiceImpl(VkProductRepository vkProductRepository,
                                VkServiceImpl vkService,
                                BlockingServiceImpl blockingService,
                                UserServiceImpl userService,
                                VkAccountServiceImpl vkAccountService,
                                UserMarketServiceImpl userMarketService, TokenServiceImpl tokenService) {
        this.vkProductRepository = vkProductRepository;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.userService = userService;
        this.vkAccountService = vkAccountService;
        this.userMarketService = userMarketService;
        this.tokenService = tokenService;
    }

    public VkProductResponseDTO getVkProductsFromDatabase(VkMarket vkMarket, int count, int page) {
        log.debug("Fetching VK products from database for market ID: {}, page: {}, count: {}", vkMarket.getId(), page, count);
        PageRequest pageRequest = PageRequest.of(page - 1, count, Sort.by("title").ascending());
        Page<UUID> vkProductsPage = vkProductRepository.findIdsByVkMarket(vkMarket, pageRequest);
        return new VkProductResponseDTO(
                vkProductsPage.getContent(),
                vkProductsPage.getTotalElements());
    }


    public List<VkProduct> getAllVkProductsFromDatabase(VkMarket vkMarket) {
        log.debug("Fetching all VK products from database for market ID: {}", vkMarket.getId());
        return vkProductRepository.findAllByVkMarket(vkMarket);
    }

    public VkProductResponseDTO convertVkProductsToIdDto(List<VkProduct> vkProducts, Long count) {
        log.debug("Converting VK products to ID DTO. Count: {}", count);
        return new VkProductResponseDTO(
                vkProducts.stream().map(VkProduct::getId).toList(),
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

        //todo разобраться потом
        public Mono<List<VkProduct>> getSynchronizedVkProducts(VkMarket vkMarket, VkAccount vkAccount) {
            Long vkMarketId = vkMarket.getMarketVkId();
            log.info("Starting synchronization for VK market ID: {}", vkMarketId);

            return tokenService.getFreshAccessToken(vkAccount, STATE).flatMap(accessToken -> {
                log.info("Fetching VK products via VK API for market ID: {}", vkMarketId);
                return vkService.getProducts(accessToken, vkMarketId)
                        .flatMapMany(Flux::fromIterable)
                        .flatMap(vkProduct -> blockingService.fromBlocking(() -> {
                            vkProduct.setVkMarket(vkMarket);
                            saveVkProduct(vkProduct);
                            return vkProduct;
                        }))
                        .collectList()
                        .doOnSuccess(products -> log.info(
                                "Successfully synchronized {} VK products",
                                products.size()))
                        .doOnError(ex -> log.error("Failed to synchronize VK products", ex));
            });
        }

    public VkProductResponseDTO getVkProducts(Long tgUserId, int count, int page) {
        log.info("Request to get VK products for Telegram user ID: {}", tgUserId);
        try {
//            User user = userService.getUserByTgId(tgUserId);
            VkAccount activeVkAccount = vkAccountService.getActiveAccount(tgUserId);
            if (activeVkAccount == null) {
                throw new IllegalStateException("No active VK account linked to this user");
            }

            UserMarket activeUserMarket = userMarketService.getActiveUserMarket(activeVkAccount.getId());
            if (activeUserMarket == null) {
                throw new IllegalStateException("No active market found for VK account");
            }

            VkMarket vkMarket = activeUserMarket.getVkMarket();
            VkProductResponseDTO productsFromDb = getVkProductsFromDatabase(vkMarket, count, page);
            List<UUID> productIds = productsFromDb.uuids();
            long totalCount = productsFromDb.count();

            if (productIds.isEmpty()) {
                log.info("No cached products found. Synchronizing from VK...");
                List<VkProduct> syncedProducts = getSynchronizedVkProducts(vkMarket, activeVkAccount).block();
                if (syncedProducts != null && !syncedProducts.isEmpty()) {
                    return convertVkProductsToIdDto(syncedProducts, totalCount);
                } else {
                    log.warn("Synchronization returned empty product list");
                    return new VkProductResponseDTO(List.of(), 0L);
                }
            }

            log.info("Returning {} products from database", productIds.size());
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
