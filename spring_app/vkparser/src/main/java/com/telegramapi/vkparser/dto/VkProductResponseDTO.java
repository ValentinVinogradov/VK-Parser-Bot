package com.telegramapi.vkparser.dto;

import java.util.List;

public record VkProductResponseDTO (
        List<VkProductDTO> products,
        Integer page,
        Long count
) {}
