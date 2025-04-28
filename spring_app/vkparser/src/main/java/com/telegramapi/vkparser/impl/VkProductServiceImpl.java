package com.telegramapi.vkparser.impl;

import com.telegramapi.vkparser.dto.VkProductDTO;
import com.telegramapi.vkparser.models.*;
import com.telegramapi.vkparser.repositories.VkProductRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Service
public class VkProductServiceImpl {
    private final VkProductRepository vkProductRepository;
    private final VkServiceImpl vkService;
    private final BlockingServiceImpl blockingService;
    private final VkMarketServiceImpl vkMarketService;
    private final UserServiceImpl userService;
    private final VkAccountServiceImpl vkAccountService;
    private final UserMarketServiceImpl userMarketService;


    public VkProductServiceImpl(VkProductRepository vkProductRepository, VkServiceImpl vkService, BlockingServiceImpl blockingService, VkMarketServiceImpl vkMarketService, UserServiceImpl userService, VkAccountServiceImpl vkAccountService, UserMarketServiceImpl userMarketService) {
        this.vkProductRepository = vkProductRepository;
        this.vkService = vkService;
        this.blockingService = blockingService;
        this.vkMarketService = vkMarketService;
        this.userService = userService;
        this.vkAccountService = vkAccountService;
        this.userMarketService = userMarketService;
    }

    public List<VkProduct> getVkProductsFromDatabase(VkMarket vkMarket) {
        return vkProductRepository.findAllByVkMarket(vkMarket);
    }

    public List<VkProductDTO> convertVkProductsToDto(List<VkProduct> vkProducts) {
        return vkProducts.stream().map(this::convertVkProductToDto).toList();
    }

    public VkProductDTO convertVkProductToDto(VkProduct vkProduct) {
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
        return vkService.getProducts(accessToken, vkMarketId)
                .flatMapMany(Flux::fromIterable)
                .flatMap(vkProduct -> blockingService.fromBlocking(() -> {
                        vkProduct.setVkMarket(vkMarket); // привязываем маркет
                        saveVkProduct(vkProduct); // сохраняем в базу
                        return vkProduct;
                    }))
                    .collectList()
                    .doOnTerminate(() ->
                            System.out.println("Vk products synchronization finished."))
                    .doOnSubscribe(s ->
                            System.out.println("Vk products synchronization started..."))
                    .doOnSuccess(s ->
                            System.out.println("Vk products synchronization completed successfully!"));
    }

    public List<VkProductDTO> getAllVkProducts(Long tgUserId) {
        User user = userService.getUserByTgId(tgUserId);
        VkAccount activeVkAccount = vkAccountService.getActiveAccount(user);
        //todo как-то передать питону что акк неактивный
        if (activeVkAccount == null) {
           throw new IllegalStateException("Account must be active");
        }
        System.out.println("Нашли активный акк: " + activeVkAccount.getFirstName());
        UserMarket activeUserMarket = userMarketService.getActiveUserMarket(activeVkAccount);
        if (activeUserMarket == null) {
            throw new IllegalStateException("Market must be active");
        }
        VkMarket vkMarket = activeUserMarket.getVkMarket();
        System.out.println("Нашли активный магазин: " + vkMarket.getMarketName());
        List<VkProduct> vkProducts = getVkProductsFromDatabase(vkMarket);
        if (vkProducts.isEmpty()) {
            String accessToken = activeVkAccount.getAccessToken();
            List<VkProduct> vkProductsFromVk = getSynchronizedVkProducts(accessToken, vkMarket).block();
            if (vkProductsFromVk != null && !vkProductsFromVk.isEmpty()) {
                System.out.println(vkProductsFromVk);
                return convertVkProductsToDto(vkProductsFromVk);
            }
        }
        System.out.println(vkProducts);
        return convertVkProductsToDto(vkProducts);
    }

    public void saveVkProduct(VkProduct vkProduct) {
        vkProductRepository.save(vkProduct);
    }
}
