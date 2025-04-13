package com.telegramapi.vkparser.controllers;

import com.telegramapi.vkparser.dto.VkTokenResponseDTO;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping("/vk")
public class LoginController {
    private final String CLIENT_ID = "53421389";
    private final String REDIRECT_URI = "http://localhost/vk/callback";
    private final String GRANT_TYPE = "authorization_code";
    private final String CODE_VERIFIER = "AOn2pwYMGOGXrXsIPU1tqZwk5L0e-RP2FiWZrnJ6DlA";

    @GetMapping("/callback")
    public ResponseEntity<VkTokenResponseDTO> callback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(name = "device_id") String deviceId,
            @RequestParam(name = "tg_id") String tgId) {
        String tokenUrl = "https://id.vk.com/oauth2/auth";

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", CLIENT_ID);
        requestBody.add("grant_type", GRANT_TYPE);
        requestBody.add("redirect_uri", REDIRECT_URI);
        requestBody.add("code", code);
        requestBody.add("state", state);
        requestBody.add("device_id", deviceId);
        requestBody.add("code_verifier", CODE_VERIFIER);



        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<VkTokenResponseDTO> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                requestEntity,
                VkTokenResponseDTO.class
        );

        VkTokenResponseDTO vkTokenResponseDTO = response.getBody();

        System.out.println("Access Token: " + vkTokenResponseDTO.getAccessToken());
        System.out.println("Refresh Token: " + vkTokenResponseDTO.getRefreshToken());

        return ResponseEntity.ok(response.getBody());
    }
}
