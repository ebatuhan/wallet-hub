package com.batu.plaid_adapter_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaidConfigurationTest {

    @Test
    void plaidClient_whenEnvironmentIsSandbox_shouldCreatePlaidApiClient() {
        PlaidConfiguration configuration = configuration("sandbox");

        assertThat(configuration.plaidClient()).isNotNull();
    }

    @Test
    void plaidClient_whenEnvironmentIsProduction_shouldCreatePlaidApiClient() {
        PlaidConfiguration configuration = configuration("production");

        assertThat(configuration.plaidClient()).isNotNull();
    }

    @Test
    void plaidClient_whenEnvironmentIsUnknown_shouldDefaultToSandboxClient() {
        PlaidConfiguration configuration = configuration("development");

        assertThat(configuration.plaidClient()).isNotNull();
    }

    private static PlaidConfiguration configuration(String environment) {
        PlaidConfiguration configuration = new PlaidConfiguration();
        ReflectionTestUtils.setField(configuration, "clientId", "client-id");
        ReflectionTestUtils.setField(configuration, "secret", "secret");
        ReflectionTestUtils.setField(configuration, "environment", environment);
        return configuration;
    }
}
