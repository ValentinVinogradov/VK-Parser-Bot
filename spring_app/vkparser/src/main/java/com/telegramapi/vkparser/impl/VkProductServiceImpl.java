package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkAccountCacheDTO;
import com.telegramapi.vkparser.dto.VkProductAIRequestDTO;
import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.dto.VkProductResponseDTO;
import com.telegramapi.vkparser.models.*;
import com.telegramapi.vkparser.repositories.VkProductRepository;
import com.telegramapi.vkparser.services.VkMarketService;
import com.telegramapi.vkparser.services.VkProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class VkProductServiceImpl implements VkProductService {
    private final String STATE = System.getenv("VK_STATE");
    private static final Logger log = LoggerFactory.getLogger(VkProductServiceImpl.class);
    private final VkProductRepository vkProductRepository;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final VkMarketService vkMarketService;
    private final VkAccountServiceImpl vkAccountService;
    private final TokenServiceImpl tokenService;
    private final RedisServiceImpl redisService;


    public VkProductServiceImpl(VkProductRepository vkProductRepository,
                                VkServiceImpl vkService,
                                BlockingServiceImpl blockingService,
                                VkMarketService vkMarketService,
                                VkAccountServiceImpl vkAccountService,
                                TokenServiceImpl tokenService,
                                RedisServiceImpl redisService) {
        this.vkProductRepository = vkProductRepository;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.vkMarketService = vkMarketService;
        this.vkAccountService = vkAccountService;
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

    public List<VkProduct> getOrSyncAllProducts(VkMarket activeVkMarket, VkAccountCacheDTO cacheVkAccount) {
        List<VkProduct> productsFromDb = getAllVkProductsFromDatabase(activeVkMarket);


        if (productsFromDb.isEmpty()) {
            log.info("No stored products found. Start synchronizing from VK...");
            syncProducts(activeVkMarket, cacheVkAccount);
            productsFromDb = getAllVkProductsFromDatabase(activeVkMarket);
        }
        return productsFromDb;
    }

    public List<VkProductAIRequestDTO> getAllVkProductsForAI(Long tgUserId) {
        log.info("Request to get all VK products for Telegram user ID: {}", tgUserId);
        try {
            VkAccountCacheDTO cacheVkAccount = vkAccountService.createVkCacheAccount(tgUserId);
            VkMarket activeVkMarket = vkMarketService.getActiveVkMarket(tgUserId, cacheVkAccount);


            List<VkProduct> vkProducts = getOrSyncAllProducts(activeVkMarket, cacheVkAccount);
            if (vkProducts.isEmpty()) {
                log.warn("Synchronization all products returned empty product list");
                return List.of();
            }

            return convertProductListToAIDTO(vkProducts);
        } catch (Exception e) {
            log.error("Failed to fetch VK products for tgUserId={}", tgUserId, e);
            throw new RuntimeException("Unable to retrieve VK products", e);
        }
    }

    private List<VkProductAIRequestDTO> convertProductListToAIDTO(List<VkProduct> vkProducts) {
        return vkProducts
                .stream()
                .map(vkProduct -> new VkProductAIRequestDTO(
                        vkProduct.getTitle(),
                        vkProduct.getPrice(),
                        vkProduct.getLikesCount(),
                        vkProduct.getRepostCount(),
                        vkProduct.getViewsCount(),
                        vkProduct.getReviewsCount(),
                        vkProduct.getCreatedAt()
                ))
                .toList();
    }

    private VkProductResponseDTO convertVkProductsToDto(List<VkProduct> vkProducts, Integer page, Long count) {
        log.debug("Converting VK products to ID DTO. Count: {}", count);
        return new VkProductResponseDTO(
                vkProducts.stream().map(this::convertVkProductToFullDto).toList(),
                page,
                count);
    }

    private VkProductDTO convertVkProductToFullDto(VkProduct vkProduct) {
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
                            .then();
                })
                .doOnSuccess(v -> log.info("Successfully synchronized VK products for market ID: {}", vkMarketId))
                .doOnError(ex -> log.error("Failed to synchronize VK products", ex))
                .block();
    }


    public VkProductResponseDTO getVkProducts(Long tgUserId, int count, int page) {
        log.info("Request to get VK products for Telegram user ID: {}", tgUserId);
        try {
            VkProductResponseDTO vkCachedProducts = getVkCachedProducts(tgUserId, page);
            if (vkCachedProducts != null) {
                return vkCachedProducts;
            }
            VkAccountCacheDTO cacheVkAccount = vkAccountService.createVkCacheAccount(tgUserId);
            VkMarket activeVkMarket = vkMarketService.getActiveVkMarket(tgUserId, cacheVkAccount);

            return getVkProductsWithCaching(tgUserId, count, page, activeVkMarket, cacheVkAccount);
        } catch (Exception e) {
            log.error("Failed to fetch VK products for tgUserId={}", tgUserId, e);
            throw new RuntimeException("Unable to retrieve VK products", e);
        }
    }

    private VkProductResponseDTO getVkProductsWithCaching(Long tgUserId, int count, int page, VkMarket activeVkMarket, VkAccountCacheDTO cacheVkAccount) {
        VkProductResponseDTO productsFromDb = getOrSyncVkProducts(count, page, activeVkMarket, cacheVkAccount);
        if (productsFromDb.products().isEmpty()) {
            log.warn("Synchronization returned empty product list");
            return new VkProductResponseDTO(List.of(), 1, 0L);
        }
        redisService.setValue(String.format("products:%s:%d", tgUserId, page), productsFromDb);

        return productsFromDb;
    }

    private VkProductResponseDTO getOrSyncVkProducts(int count, int page, VkMarket activeVkMarket, VkAccountCacheDTO cacheVkAccount) {
        VkProductResponseDTO productsFromDb = getVkProductsFromDatabase(activeVkMarket, count, page);


        if (productsFromDb.products().isEmpty()) {
            log.info("No stored products found. Synchronizing from VK...");
            syncProducts(activeVkMarket, cacheVkAccount);
            productsFromDb = getVkProductsFromDatabase(activeVkMarket, count, page);
        }
        return productsFromDb;
    }

    private VkProductResponseDTO getVkCachedProducts(Long tgUserId, int page) {
        VkProductResponseDTO vkCachedProducts = redisService
                .getValue(String.format("products:%s:%d", tgUserId, page), VkProductResponseDTO.class);
        log.info("Cached vk products: {}", vkCachedProducts);
        return vkCachedProducts;
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
