package com.telegramapi.vkparser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telegramapi.vkparser.dto.VkRefreshResponseDTO;
import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.dto.VkUserInfoDTO;
import com.telegramapi.vkparser.models.VkMarket;
import com.telegramapi.vkparser.models.VkProduct;
import com.telegramapi.vkparser.services.VkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class VkServiceImpl implements VkService {
    private final String CLIENT_ID = System.getenv("VK_CLIENT_ID");
    private final String REDIRECT_URI = System.getenv("VK_CLEAN_REDIRECT_URI");
    private final String GRANT_TYPE_CODE = System.getenv("VK_GRANT_TYPE_CODE");
    private final String GRANT_TYPE_TOKEN = System.getenv("VK_GRANT_TYPE_TOKEN");
    private final String CODE_VERIFIER = System.getenv("VK_CODE_VERIFIER");
    private final String TOKEN_URL = System.getenv("VK_TOKEN_URL");
    private final String LOGOUT_URL = System.getenv("VK_LOGOUT_URL");
    private final String VK_URL = System.getenv("VK_URL");
    private final String VK_API_VERSION = System.getenv("VK_API_VERSION");

    private final WebClient vkWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(VkServiceImpl.class);



    public VkServiceImpl(WebClient vkWebClient) {
        this.vkWebClient = vkWebClient;
    }

    public Mono<VkTokenResponseDTO> getUserTokens(String code, String state, String deviceId) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", CLIENT_ID);
        requestBody.add("grant_type", GRANT_TYPE_CODE);
        requestBody.add("redirect_uri", REDIRECT_URI);
        requestBody.add("code", code);
        requestBody.add("state", state);
        requestBody.add("device_id", deviceId);
        requestBody.add("code_verifier", CODE_VERIFIER);

        log.info("Attempting to fetch user tokens with code={}, state={}, deviceId={}", code, state, deviceId);

        return vkWebClient
            .post()
            .uri(TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(VkTokenResponseDTO.class)
            .doOnSubscribe(subscription -> log.info("Fetching user tokens..."))
            .doOnSuccess(response -> log.info("User tokens received successfully: accessToken={}, expiresIn={}",
                    response.accessToken(), response.expiresIn()))
            .doOnError(error -> log.error("Failed to fetch user tokens", error));
    }

    public Mono<VkRefreshResponseDTO> refreshAccessToken(String refreshToken, String state, String deviceId) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", GRANT_TYPE_TOKEN);
        requestBody.add("refresh_token", refreshToken);
        requestBody.add("client_id", CLIENT_ID);
        requestBody.add("device_id", deviceId);
        requestBody.add("state", state);

        log.info("Attempting to refresh user tokens with refreshToken=*****, state={}, deviceId={}", state, deviceId);

        return vkWebClient
            .post()
            .uri(TOKEN_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(VkRefreshResponseDTO.class)
            .doOnSubscribe(subscription -> log.info("Refreshing user tokens..."))
            .doOnSuccess(response -> log.info("User tokens refreshed successfully: newAccessToken={}, expiresIn={}",
                    response.accessToken(), response.expiresIn()))
            .doOnError(error -> log.error("Failed to refresh user tokens", error));
    }

    public Mono<Void> logout(String accessToken) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", CLIENT_ID);
        requestBody.add("access_token", accessToken);

        log.info("Sending VK logout request for accessToken: {}", accessToken);

        return vkWebClient
                .post()
                .uri(LOGOUT_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String.class)
                .doOnNext(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("VK logout successful: {}", response.getBody());
                    } else {
                        log.warn("VK logout returned non-success status: {}", response.getStatusCode());
                    }
                })
                .doOnError(error -> log.error("VK logout failed: {}", error.getMessage(), error))
                .then(); // завершает как Mono<Void>
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
            .doOnSubscribe(subscription ->
                    log.info("Fetching VK products for vkMarketId={}", vkMarketId))
            .doOnSuccess(resp ->
                    log.info("Successfully received VK products response"))
            .doOnError(e ->
                    log.error("Error after response from VK API: {}", e.getMessage(), e))
            .flatMap(response -> {
                JsonNode itemsNode = response.path("response").path("items");

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
                                if ("x".equals(size.path("type").asText())) {
                                    photoUrls.add(size.path("url").asText());
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
                    .doOnSubscribe(s ->
                            log.info("Started parsing VK products..."))
                    .doOnSuccess(s ->
                            log.info("Parsing VK products completed successfully with {} products", s.size()))
                    .doOnTerminate(() ->
                            log.info("Finished parsing VK products"))
                    .doOnError(e ->
                            log.error("Error during parsing VK products: {}", e.getMessage(), e));
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
                        .queryParam("fields", "members_count")
                        .queryParam("v", VK_API_VERSION)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSubscribe(subscription ->
                        log.info("Fetching VK groups for vkUserId={}...", vkUserId))
                .doOnError(e ->
                        log.error("Error after response from VK API: {}", e.getMessage(), e))
                .doOnSuccess(response ->
                        log.info("Successfully received VK groups response!"))
                .flatMap(response -> {
                    JsonNode itemsNode = response.path("response").path("items");
                    log.info("Found {} VK groups", itemsNode.size());

                    return Flux.fromIterable(itemsNode)
                            .map(item -> {
                                long vkMarketId = item.path("id").asLong();
                                int membersCount = item.path("members_count").asInt();
                                String vkMarketName = item.path("name").asText();
                                String vkMarketScreenName = item.path("screen_name").asText();

                                VkMarket vkMarket = new VkMarket();
                                vkMarket.setMarketName(vkMarketName);
                                vkMarket.setMarketVkId(vkMarketId);
                                vkMarket.setMembersCount(membersCount);
                                vkMarket.setMarketUrl(VK_URL + "/" + vkMarketScreenName);
                                return vkMarket;
                            })
                            .collectList()
                            .doOnSubscribe(s ->
                                    log.info("Started parsing VK groups..."))
                            .doOnSuccess(list ->
                                    log.info("Parsing VK groups completed successfully with {} groups", list.size()))
                            .doOnTerminate(() ->
                                    log.info("Finished parsing VK groups."))
                            .doOnError(e ->
                                    log.error("Error during parsing VK groups: {}", e.getMessage(), e));
                });
    }

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
                .map(json -> json.get("response"))
                .map(responseNode -> objectMapper.convertValue(responseNode, VkUserInfoDTO.class))
                .doOnSubscribe(subscription ->
                        log.info("Fetching VK user profile info..."))
                .doOnError(error ->
                        log.error("Error fetching VK user profile info: {}", error.getMessage(), error))
                .doOnSuccess(vkUserInfoDTO ->
                        log.info("Successfully fetched VK user profile info: screenName={}", vkUserInfoDTO.screenName()));
    }
}
