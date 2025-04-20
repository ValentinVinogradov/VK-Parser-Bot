package com.telegramapi.vkparser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import com.telegramapi.vkparser.models.UserMarket;
import com.telegramapi.vkparser.models.VkMarket;
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
import java.util.List;

@Service
public class VkServiceImpl implements VkService {
    private final String CLIENT_ID = System.getenv("VK_CLIENT_ID");
    private final String REDIRECT_URI = System.getenv("VK_CLEAN_REDIRECT_URI");
    private final String GRANT_TYPE = System.getenv("VK_GRANT_TYPE");
    private final String CODE_VERIFIER = System.getenv("VK_CODE_VERIFIER");
    private final String TOKEN_URL = System.getenv("VK_TOKEN_URL");
    private final String VK_API_VERSION = System.getenv("VK_API_VERSION");
    private final WebClient vkWebClient;

    private final VkMarketServiceImpl vkMarketService;
    private final BlockingServiceImpl blockingServiceImpl;


    public VkServiceImpl(WebClient vkWebClient,
                         VkMarketServiceImpl vkMarketService, BlockingServiceImpl blockingServiceImpl) {
        this.vkWebClient = vkWebClient;
        this.vkMarketService = vkMarketService;
        this.blockingServiceImpl = blockingServiceImpl;
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

    public Mono<List<UserMarket>> getUserMarkets(Long tgUserId, Long vkUserId, String accessToken) {
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
                .println("Fetching VK groups for vkUserId=" + vkUserId + ", tgUserId=" + tgUserId + "..."))
            .doOnSuccess(resp -> System.out.println("Successful market response!"))
            .doOnError(e -> System.out.println("Error after response: " + e))
            .doOnSuccess(response -> System.out.println("Successfully received VK response!"))
            .flatMap(response -> {
                JsonNode itemsNode = response.path("response").path("items");
                System.out.println("Found " + itemsNode.size() + " VK groups!");

                return Flux.fromIterable(itemsNode)
                    .flatMap(item -> {
                        Long vkMarketId = item.path("id").asLong();
                        String vkMarketName = item.path("name").asText();
                        System.out.println("Processing market: " + vkMarketId + " - " + vkMarketName + "...");

                        UserMarket userMarket = new UserMarket();

                        return blockingServiceImpl.fromBlocking(() ->
                                        vkMarketService.existsByVkId(vkMarketId))
                            .flatMap(exists -> {
                                if (!exists) {
                                    System.out
                                            .println("Creating new market: "
                                                    + vkMarketId + " - " + vkMarketName + "...");
                                    VkMarket vkMarket = vkMarketService.createVkMarket(vkMarketId, vkMarketName);
                                    return blockingServiceImpl.fromBlocking(() ->
                                                    vkMarketService.saveVkMarket(vkMarket))
                                        .doOnNext(saved -> System.out.println("Saved new market: " + saved));
                                } else {
                                    return blockingServiceImpl.fromBlocking(() ->
                                                    vkMarketService.getMarketById(vkMarketId))
                                        .doOnNext(existing -> System.out
                                                .println("Fetched existing market: " + existing));
                                }
                            })
                            .map(vkMarket -> {
                                userMarket.setTgUserId(tgUserId);
                                userMarket.setVkUserId(vkUserId);
                                userMarket.setVkMarket(vkMarket);
                                System.out.println("Created UserMarket entry: " + userMarket);
                                return userMarket;
                            });
                    }).collectList()
                    .doOnNext(userMarkets -> System.out.println("Total UserMarkets created: " + userMarkets.size()))
                    .doOnTerminate(() -> System.out.println("User Markets fetching finished."));
            });
    }
}
