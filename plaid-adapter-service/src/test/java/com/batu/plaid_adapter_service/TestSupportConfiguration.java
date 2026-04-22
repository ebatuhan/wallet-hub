package com.batu.plaid_adapter_service;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

@TestConfiguration(proxyBeanMethods = false)
public class TestSupportConfiguration {

    @Bean
    OAuth2AuthorizedClientRepository oauth2AuthorizedClientRepository() {
        return mock(OAuth2AuthorizedClientRepository.class);
    }
}
