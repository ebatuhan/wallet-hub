package com.batu.ai_assistant.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank String message
) {}
