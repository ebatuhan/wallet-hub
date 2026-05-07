package com.batu.ai_assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequestDTO(
        @NotBlank @Size(max = 4000) String message
) {}
