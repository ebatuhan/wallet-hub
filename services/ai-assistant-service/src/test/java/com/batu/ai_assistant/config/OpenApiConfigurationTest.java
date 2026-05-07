package com.batu.ai_assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigurationTest {

    @Test
    void aiAssistantServiceOpenAPI_whenCreated_shouldExposeServiceMetadataAndBearerAuth() {
        var openApi = new OpenApiConfiguration().aiAssistantServiceOpenAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Wallet Hub AI Assistant Service API");
        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfiguration.BEARER_AUTH);
        assertThat(openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.BEARER_AUTH).getType())
                .isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.BEARER_AUTH).getScheme())
                .isEqualTo("bearer");
    }
}
