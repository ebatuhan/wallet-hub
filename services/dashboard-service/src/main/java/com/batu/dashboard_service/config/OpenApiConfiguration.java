package com.batu.dashboard_service.config;

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
    OpenAPI dashboardServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wallet Hub Dashboard Service API")
                        .version("v1")
                        .description("User, account, transaction, and budget dashboard summary endpoints."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuthScheme()));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }
}
