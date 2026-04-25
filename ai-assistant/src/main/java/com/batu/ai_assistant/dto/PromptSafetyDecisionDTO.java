package com.batu.ai_assistant.dto;

public record PromptSafetyDecisionDTO(
        PromptSafetyDecisionType decision,
        String reason
) {
}
