package com.batu.plaid_adapter_service.config;

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
    OpenAPI plaidAdapterServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Hub Plaid Adapter Service API")
                        .version("v1")
                        .description("Plaid link-token, exchange, connection management, and webhook endpoints."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuthScheme()));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
