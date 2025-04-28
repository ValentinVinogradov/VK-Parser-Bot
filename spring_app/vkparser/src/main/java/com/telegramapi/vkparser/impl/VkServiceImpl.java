package com.telegramapi.vkparser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.models.VkProduct;
import com.telegramapi.vkparser.services.VkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class VkServiceImpl implements VkService {
    private final String CLIENT_ID = System.getenv("VK_CLIENT_ID");
    private final String REDIRECT_URI = System.getenv("VK_CLEAN_REDIRECT_URI");
    private final String GRANT_TYPE = System.getenv("VK_GRANT_TYPE");
    private final String CODE_VERIFIER = System.getenv("VK_CODE_VERIFIER");
    private final String TOKEN_URL = System.getenv("VK_TOKEN_URL");
    private final String VK_URL = System.getenv("VK_URL");
    private final String VK_API_VERSION = System.getenv("VK_API_VERSION");
    private final WebClient vkWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();



    public VkServiceImpl(WebClient vkWebClient) {
        this.vkWebClient = vkWebClient;
    }

    public Mono<VkTokenResponseDTO> getUserTokens(String code, String state, String deviceId) {


        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", CLIENT_ID);
        requestBody.add("grant_type", GRANT_TYPE);
        requestBody.add("redirect_uri", REDIRECT_URI);
        requestBody.add("code", code);
        requestBody.add("state", state);
        requestBody.add("device_id", deviceId);
        requestBody.add("code_verifier", CODE_VERIFIER);

        return vkWebClient
                .post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(VkTokenResponseDTO.class)
                .doOnSubscribe(subscription -> System.out
                        .println("Fetching user tokens..."))
                .doOnSuccess((ignored) -> System.out.println("User tokens received successfully!"));
    }

    public Mono<List<VkProduct>> getProducts(String accessToken, Long vkMarketId) {
        return vkWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/method/market.get")
                        .queryParam("access_token", accessToken)
                        .queryParam("owner_id", -vkMarketId)
                        .queryParam("extended", 1)
                        .queryParam("v", VK_API_VERSION)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSubscribe(subscription -> System.out.println("Fetching VK products for vkMarketId=" + vkMarketId))
                .doOnSuccess(resp -> System.out.println("Successfully received VK products response!"))
                .doOnError(e -> System.out.println("Error after response: " + e.getMessage()))
                .flatMap(response -> {
                    JsonNode itemsNode = response.path("response").path("items");

                    //todo подумать насчет количества
                    int count = response.path("response").path("count").asInt();
                    System.out.println("Found " + count + " VK products");
                    return Flux.fromIterable(itemsNode)
                            .map(item -> {
                                long vkProductId = item.path("id").asLong();
                                String title = item.path("title").asText();
                                String category = item.path("category").path("name").asText();
                                String description = item.path("description").asText();
                                String price = item.path("price").path("text").asText();
                                List<String> photoUrls = new ArrayList<>();
                                for (JsonNode photo : item.path("photos")) {
                                    for (JsonNode size : photo.path("sizes")) {
                                        if (size.path("type").asText().equals("x")) {
                                            String url = size.path("url").asText();
                                            photoUrls.add(url);
                                        }
                                    }
                                }
                                int availability = item.path("availability").asInt();
                                int stockQuantity = item.path("stock_amount").asInt();
                                int likesCount = item.path("likes").path("count").asInt();
                                int repostsCount = item.path("reposts").path("count").asInt();
                                int reviewsCount = item.path("item_rating").path("reviews_count").asInt();
                                int viewsCount = item.path("views_count").asInt();
                                double rating = item.path("item_rating").path("rating").asDouble();
                                long timestamp = item.path("date").asLong();
                                Instant createdAt = Instant.ofEpochSecond(timestamp);

                                VkProduct vkProduct = new VkProduct();
                                vkProduct.setVkProductId(vkProductId);
                                vkProduct.setTitle(title);
                                vkProduct.setCategory(category);
                                vkProduct.setDescription(description);
                                vkProduct.setPrice(price);
                                vkProduct.setPhotoUrls(photoUrls);
                                vkProduct.setAvailability(availability);
                                vkProduct.setStockQuantity(stockQuantity);
                                vkProduct.setLikesCount(likesCount);
                                vkProduct.setRepostCount(repostsCount);
                                vkProduct.setReviewsCount(reviewsCount);
                                vkProduct.setViewsCount(viewsCount);
                                vkProduct.setRating(rating);
                                vkProduct.setCreatedAt(createdAt);

                                return vkProduct;
                            })
                            .collectList()
                            .doOnTerminate(() -> System.out.println("Finished parsing VK products."))
                            .doOnSubscribe(s -> System.out.println("Started parsing VK products..."))
                            .doOnSuccess(s -> System.out.println("Parsing VK products completed successfully!"))
                            .doOnError(e -> System.out.println("Error after parsing VK groups: " + e.getMessage()));
                });
    }

    public Mono<List<VkMarket>> getUserMarkets(Long vkUserId, String accessToken) {
        return vkWebClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/method/groups.get")
                .queryParam("access_token", accessToken)
                .queryParam("user_id", vkUserId)
                .queryParam("filter", "admin")
                .queryParam("extended", 1)
                .queryParam("v", VK_API_VERSION)
                .build())
            .retrieve()
            .bodyToMono(JsonNode.class)
            .doOnSubscribe(subscription -> System.out
                .println("Fetching VK groups for vkUserId=" + vkUserId + "..."))
            .doOnError(e -> System.out.println("Error after response: " + e))
            .doOnSuccess(response -> System.out.println("Successfully received VK response!"))
            .flatMap(response -> {
                JsonNode itemsNode = response.path("response").path("items");
                System.out.println("Found " + itemsNode.size() + " VK groups!");

                return Flux.fromIterable(itemsNode)
                        .map(item -> {
                            long vkMarketId = item.path("id").asLong();
                            String vkMarketName = item.path("name").asText();
                            String vkMarketScreenName = item.path("screen_name").asText();
                            VkMarket vkMarket = new VkMarket();
                            vkMarket.setMarketName(vkMarketName);
                            vkMarket.setMarketVkId(vkMarketId);
                            vkMarket.setMarketUrl(VK_URL + "/" + vkMarketScreenName);
                            return vkMarket;
                        })
                        .collectList()
                        .doOnTerminate(() -> System.out.println("Finished parsing VK groups."))
                        .doOnSubscribe(s -> System.out.println("Started parsing VK groups..."))
                        .doOnSuccess(s -> System.out.println("Parsing VK groups completed successfully!"))
                        .doOnError(e -> System.out.println("Error after parsing VK groups: " + e.getMessage()));
            });
    }


    //todo добавить path response
    public Mono<VkUserInfoDTO> getUserProfileInfo(String accessToken) {
        return vkWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/method/account.getProfileInfo")
                        .queryParam("access_token", accessToken)
                        .queryParam("v", VK_API_VERSION)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("response")) // <-- заходим в "response"
                .map(responseNode -> objectMapper.convertValue(responseNode, VkUserInfoDTO.class))
                .doOnSubscribe(subscription -> System.out.println("Fetching VK user profile info..."))
                .doOnError(error -> System.err.println("Error fetching VK user profile info: " + error.getMessage()))
                .doOnSuccess(vkUserInfoDTO ->
                        System.out.println("Successfully fetched VK user profile info! " + vkUserInfoDTO.getScreenName()));
    }


}
