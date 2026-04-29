package com.batu.plaid_adapter_service.config;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import feign.RequestInterceptor;

@Configuration
public class InternalServiceClientConfiguration {

    @Bean
    RequestInterceptor internalServiceAuthorizationInterceptor(InternalServiceTokenProvider tokenProvider) {
        return template -> template.header("Authorization", "Bearer " + tokenProvider.accessToken());
    }

    @Bean
    InternalServiceTokenProvider internalServiceTokenProvider(
            @Value("${internalclient.token-url}") String tokenUrl,
            @Value("${internalclient.client-id}") String clientId,
            @Value("${internalclient.client-secret}") String clientSecret) {
        return new InternalServiceTokenProvider(tokenUrl, clientId, clientSecret, RestClient.create());
    }

    static class InternalServiceTokenProvider {
        private final String tokenUrl;
        private final String clientId;
        private final String clientSecret;
        private final RestClient restClient;

        private String accessToken;
        private Instant expiresAt = Instant.EPOCH;

        InternalServiceTokenProvider(String tokenUrl, String clientId, String clientSecret, RestClient restClient) {
            this.tokenUrl = tokenUrl;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.restClient = restClient;
        }

        synchronized String accessToken() {
            if (accessToken == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
                refreshToken();
            }

            return accessToken;
        }

        private void refreshToken() {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            TokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.access_token() == null || response.access_token().isBlank()) {
                throw new IllegalStateException("Unable to obtain internal service token");
            }

            accessToken = response.access_token();
            expiresAt = Instant.now().plusSeconds(response.expires_in() == null ? 60 : response.expires_in());
        }
    }

    record TokenResponse(String access_token, Long expires_in) {
    }
}
