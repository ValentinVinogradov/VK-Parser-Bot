package com.telegramapi.vkparser.dto;

import java.util.List;
import java.util.UUID;

public record VkProductResponseDTO (
        List<VkProductDTO> products,
        Integer page,
        Long count
) {}
