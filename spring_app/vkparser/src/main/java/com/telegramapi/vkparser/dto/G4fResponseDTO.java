package com.telegramapi.vkparser.dto;

import java.util.List;

public record G4fResponseDTO(List<Choice> choices) {

    public record Choice(Message message) { }

    public record Message(String role, String content) { }

}
