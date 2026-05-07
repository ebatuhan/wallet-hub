package com.batu.ai_assistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfiguration {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI aiAssistantServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Hub AI Assistant Service API")
                        .version("v1")
                        .description("AI assistant chat and chat history endpoints."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuthScheme()));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
