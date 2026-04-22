package com.batu.ai_assistant.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        UUID conversationId,
        @NotBlank String message
) {}
