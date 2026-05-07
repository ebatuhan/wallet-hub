package com.batu.ai_assistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import feign.RequestTemplate;

class CallerJwtFeignConfigurationTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void callerJwtRelayInterceptor_whenAuthenticationIsJwt_shouldAddBearerToken() {
        Jwt jwt = Jwt.withTokenValue("caller-token")
                .header("alg", "none")
                .subject("95000000-0000-0000-0000-000000000001")
                .issuedAt(Instant.parse("2026-05-07T10:00:00Z"))
                .expiresAt(Instant.parse("2026-05-07T11:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        RequestTemplate template = new RequestTemplate();

        new CallerJwtFeignConfiguration().callerJwtRelayInterceptor().apply(template);

        assertThat(template.headers()).containsEntry("Authorization", List.of("Bearer caller-token"));
    }

    @Test
    void callerJwtRelayInterceptor_whenAuthenticationIsNotJwt_shouldLeaveHeadersUnchanged() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "password"));
        RequestTemplate template = new RequestTemplate();

        new CallerJwtFeignConfiguration().callerJwtRelayInterceptor().apply(template);

        assertThat(template.headers()).isEqualTo(Map.of());
    }
}
