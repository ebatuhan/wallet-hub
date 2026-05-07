package com.batu.plaid_adapter_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import feign.RequestInterceptor;
import feign.RequestTemplate;

class ServiceTokenFeignConfigurationTest {

    private final ServiceTokenFeignConfiguration configuration = new ServiceTokenFeignConfiguration();

    @Test
    void serviceTokenInterceptor_whenClientIsAuthorized_shouldAddBearerToken() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "service-token",
                Instant.parse("2026-05-08T00:00:00Z"),
                Instant.parse("2026-05-08T01:00:00Z"));
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(manager.authorize(argThat(request -> request != null
                && request.getClientRegistrationId().equals("wallet-hub-internal")
                && request.getPrincipal().getName().equals("plaid-adapter-service"))))
                .thenReturn(authorizedClient);
        RequestInterceptor interceptor = configuration.serviceTokenInterceptor(manager);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer service-token"));
        verify(manager).authorize(argThat(request -> request instanceof OAuth2AuthorizeRequest));
    }

    @Test
    void serviceTokenInterceptor_whenClientCannotBeAuthorized_shouldThrowException() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        when(manager.authorize(argThat(request -> request != null))).thenReturn(null);
        RequestInterceptor interceptor = configuration.serviceTokenInterceptor(manager);

        assertThatThrownBy(() -> interceptor.apply(new RequestTemplate()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to authorize internal service client");
    }
}
