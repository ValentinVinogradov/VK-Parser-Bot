package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.dto.VkProductResponseDTO;
import com.telegramapi.vkparser.models.*;
import com.telegramapi.vkparser.repositories.VkProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;


@Service
public class VkProductServiceImpl {
    private final VkProductRepository vkProductRepository;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final UserServiceImpl userService;
    private final VkAccountServiceImpl vkAccountService;
    private final UserMarketServiceImpl userMarketService;


    public VkProductServiceImpl(VkProductRepository vkProductRepository,
                                VkServiceImpl vkService,
                                BlockingServiceImpl blockingService,
                                UserServiceImpl userService,
                                VkAccountServiceImpl vkAccountService,
                                UserMarketServiceImpl userMarketService) {
        this.vkProductRepository = vkProductRepository;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.userService = userService;
        this.vkAccountService = vkAccountService;
        this.userMarketService = userMarketService;
    }

    public VkProductResponseDTO getVkProductsFromDatabase(VkMarket vkMarket, int count, int page) {
        System.out.println("page: " + page);
        PageRequest pageRequest = PageRequest.of(page - 1, count, Sort.by("title").ascending());
        Page<UUID> vkProductsPage = vkProductRepository.findIdsByVkMarket(vkMarket, pageRequest);
        return new VkProductResponseDTO(
                vkProductsPage.getContent(),
                vkProductsPage.getTotalElements());
    }

    public List<VkProduct> getAllVkProductsFromDatabase(VkMarket vkMarket) {
        return vkProductRepository.findAllByVkMarket(vkMarket);
    }

    public VkProductResponseDTO convertVkProductsToIdDto(List<VkProduct> vkProducts, Long count) {
        return new VkProductResponseDTO(
                vkProducts.stream().map(VkProduct::getId).toList(),
                count);
    }


    public VkProductDTO convertVkProductToFullDto(VkProduct vkProduct) {
        VkProductDTO vkProductDTO = new VkProductDTO();
        vkProductDTO.setId(vkProduct.getId());
        vkProductDTO.setTitle(vkProduct.getTitle());
        vkProductDTO.setCategory(vkProduct.getCategory());
        vkProductDTO.setDescription(vkProduct.getDescription());
        vkProductDTO.setPrice(vkProduct.getPrice());
        vkProductDTO.setAvailability(vkProduct.getAvailability());
        vkProductDTO.setPhotoUrls(vkProduct.getPhotoUrls());
        vkProductDTO.setStockQuantity(vkProduct.getStockQuantity());
        vkProductDTO.setLikesCount(vkProduct.getLikesCount());
        vkProductDTO.setRepostCount(vkProduct.getRepostCount());
        vkProductDTO.setViewsCount(vkProduct.getViewsCount());
        vkProductDTO.setReviewsCount(vkProduct.getReviewsCount());
        vkProductDTO.setCreatedAt(vkProduct.getCreatedAt());
        return vkProductDTO;
    }

    public Mono<List<VkProduct>> getSynchronizedVkProducts(String accessToken, VkMarket vkMarket) {
        Long vkMarketId = vkMarket.getMarketVkId();
        System.out.println("Зашли в процесс получения через вк");

        return vkService.getProducts(accessToken, vkMarketId) // Mono<List<VkProduct>>
                .flatMapMany(Flux::fromIterable) // превращаем Mono<List<...>> в Flux<...>
                .flatMap(vkProduct -> blockingService.fromBlocking(() -> {
                    vkProduct.setVkMarket(vkMarket);
                    saveVkProduct(vkProduct);
                    return vkProduct;
                }))
                .collectList()
                .doOnSubscribe(s -> System.out.println("Vk products synchronization started..."))
                .doOnSuccess(s -> System.out.println("Vk products synchronization completed successfully!"))
                .doOnTerminate(() -> System.out.println("Vk products synchronization finished."));
    }


    public VkProductResponseDTO getVkProducts(Long tgUserId, int count, int page) {
        User user = userService.getUserByTgId(tgUserId);
        VkAccount activeVkAccount = vkAccountService.getActiveAccount(user);
        if (activeVkAccount == null) {
           throw new IllegalStateException("Account must be active");
        }
        UserMarket activeUserMarket = userMarketService.getActiveUserMarket(activeVkAccount.getId());
        if (activeUserMarket == null) {
            throw new IllegalStateException("Market must be active");
        }
        VkMarket vkMarket = activeUserMarket.getVkMarket();
        VkProductResponseDTO vkProductsIdsFromDatabase = getVkProductsFromDatabase(vkMarket, count, page);
        List<UUID> productIds = vkProductsIdsFromDatabase.uuids();
        long totalCount = vkProductsIdsFromDatabase.count();
        if (productIds.isEmpty()) {
            String accessToken = activeVkAccount.getAccessToken();
            List<VkProduct> vkProductsFromVk = getSynchronizedVkProducts(accessToken, vkMarket).block();


            if (!vkProductsFromVk.isEmpty()) {
                return convertVkProductsToIdDto(vkProductsFromVk, totalCount);
            } else {
                //todo
                return null;
            }

        }
        return vkProductsIdsFromDatabase;
    }

    public void saveVkProduct(VkProduct vkProduct) {
        vkProductRepository.save(vkProduct);
    }

    public VkProductDTO getVkProductById(UUID vkProductId) {
        VkProduct vkProduct = vkProductRepository.findById(vkProductId)
                .orElse(null);
        if (vkProduct != null) {

            return convertVkProductToFullDto(vkProduct);
        } else {
            //todo
            return null;
        }
    }
}
