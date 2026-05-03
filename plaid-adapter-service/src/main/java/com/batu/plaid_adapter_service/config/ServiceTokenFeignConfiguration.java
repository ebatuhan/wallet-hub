package com.batu.plaid_adapter_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import feign.RequestInterceptor;

@Configuration
public class ServiceTokenFeignConfiguration {
    private static final String INTERNAL_CLIENT = "wallet-hub-internal";
    private static final String PRINCIPAL = "plaid-adapter-service";

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean
    RequestInterceptor serviceTokenInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        return template -> {
            var authorizedClient = authorizedClientManager.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId(INTERNAL_CLIENT)
                    .principal(PRINCIPAL)
                    .build());
            if (authorizedClient == null) {
                throw new IllegalStateException("Unable to authorize internal service client");
            }

            template.header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + authorizedClient.getAccessToken().getTokenValue());
        };
    }
}
